/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.backupconfig.BaseBackupPerformance;
import io.stackgres.apiweb.dto.cluster.ClusterBackupsConfiguration;
import io.stackgres.apiweb.dto.cluster.ClusterConfiguration;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterScriptFrom;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StackGresKubernetesMockServerSetup;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class ClusterResourceQuarkusTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  private final StackGresCluster cluster = getCluster();

  private final ClusterDto clusterDto = getClusterInlineScripts();

  @BeforeEach
  void setUp() {
    mockServer.getClient().secrets().inNamespace("test").delete();
    mockServer.getClient().configMaps().inNamespace("test").delete();

    clusterDto.getMetadata().setNamespace("test");
    cluster.getMetadata().setNamespace("test");
    cluster.getMetadata().setName(StringUtils.getRandomClusterName());
    cluster.getSpec().setConfiguration(new StackGresClusterConfiguration());
    mockServer.getClient().resources(
        StackGresCluster.class,
        StackGresClusterList.class)
        .inNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getMetadata().getName())
        .create(cluster);

    Service primary = new ServiceBuilder()
        .withNewMetadata()
        .withName(PatroniUtil.readWriteName(cluster.getMetadata().getName()))
        .withNamespace(cluster.getMetadata().getNamespace())
        .endMetadata()
        .withNewSpec()
        .withType(cluster.getSpec().getPostgresServices().getPrimary().getType() != null
            ? cluster.getSpec().getPostgresServices().getPrimary().getType()
            : null)
        .withClusterIP("10.10.100.8")
        .endSpec()
        .build();
    Service replicas = new ServiceBuilder()
        .withNewMetadata()
        .withName(PatroniUtil.readOnlyName(cluster.getMetadata().getName()))
        .withNamespace(cluster.getMetadata().getNamespace())
        .endMetadata()
        .withNewSpec()
        .withType(cluster.getSpec().getPostgresServices().getReplicas().getType() != null
            ? cluster.getSpec().getPostgresServices().getReplicas().getType()
            : null)
        .withClusterIP("10.10.100.30")
        .endSpec()
        .build();
    mockServer.getClient().services()
        .inNamespace(cluster.getMetadata().getNamespace()).create(primary);
    mockServer.getClient().services()
        .inNamespace(cluster.getMetadata().getNamespace()).create(replicas);
  }

  @AfterEach
  void tearDown() {
    mockServer.getClient().secrets().inNamespace("test").delete();
    mockServer.getClient().configMaps().inNamespace("test").delete();
    mockServer.getClient().resources(
        StackGresCluster.class,
        StackGresClusterList.class)
        .inNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getMetadata().getName())
        .delete();
    mockServer.getClient().resources(
        StackGresCluster.class,
        StackGresClusterList.class)
        .inNamespace(clusterDto.getMetadata().getNamespace())
        .withName(clusterDto.getMetadata().getName())
        .delete();
  }

  private ClusterDto getClusterInlineScripts() {
    return JsonUtil.readFromJson("stackgres_cluster/inline_scripts.json", ClusterDto.class);
  }

  private StackGresCluster getCluster() {
    return JsonUtil.readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
  }

  @Test
  void givenACreationWithInlineScripts_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .body(clusterDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then()
        .log().all()
        .statusCode(204);
  }

  @Test
  void getCluster_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .pathParam("namespace", cluster.getMetadata().getNamespace())
        .pathParam("name", cluster.getMetadata().getName())
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/namespaces/{namespace}/sgclusters/{name}")
        .then()
        .body("metadata.namespace", equalTo("test"),
            "metadata.name", equalTo(cluster.getMetadata().getName()),
            "spec.instances", equalTo(1),
            "spec.postgres.version", equalTo("13.4"),
            "spec.sgInstanceProfile", equalTo("size-xs"),
            "info.superuserSecretName", equalTo(cluster.getMetadata().getName()),
            "info.superuserPasswordKey", equalTo("superuser-password"),
            "info.primaryDns",
            equalTo(PatroniUtil.readWriteName(cluster.getMetadata().getName())
                + ".test"),
            "info.replicasDns",
            equalTo(PatroniUtil.readOnlyName(cluster.getMetadata().getName())
                + ".test"))
        .statusCode(200);
  }

  @Test
  void getListCluster_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/sgclusters")
        .then()
        .body("[0].metadata.namespace", equalTo("test"),
            "[0].metadata.name", equalTo(cluster.getMetadata().getName()),
            "[0].spec.instances", equalTo(1),
            "[0].spec.postgres.version", equalTo("13.4"),
            "[0].spec.sgInstanceProfile", equalTo("size-xs"),
            "[0].info.superuserSecretName", equalTo(cluster.getMetadata().getName()),
            "[0].info.superuserPasswordKey", equalTo("superuser-password"),
            "[0].info.primaryDns",
            equalTo(PatroniUtil.readWriteName(cluster.getMetadata().getName())
                + ".test"),
            "[0].info.replicasDns",
            equalTo(PatroniUtil.readOnlyName(cluster.getMetadata().getName())
                + ".test"))
        .statusCode(200);
  }

  @Test
  void givenACreationWithConfigMapsScripts_shouldNotFail() {
    ClusterScriptEntry entry = getConfigMapScriptEntry();
    clusterDto.getSpec().getInitData().setScripts(Collections.singletonList(entry));

    given()
        .header(AUTHENTICATION_HEADER)
        .body(clusterDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);

    ConfigMap configMap = mockServer.getClient().configMaps().inNamespace("test")
        .withName(entry.getScriptFrom().getConfigMapKeyRef().getName())
        .get();
    assertNotNull(configMap);

    String actualConfigScript =
        configMap.getData().get(entry.getScriptFrom().getConfigMapKeyRef().getKey());
    assertEquals(entry.getScriptFrom().getConfigMapScript(), actualConfigScript);
  }

  @Test
  void givenACreationWithSecretScripts_shouldNotFail() {
    ClusterScriptEntry entry = getSecretScriptEntry();
    clusterDto.getSpec().getInitData().setScripts(Collections.singletonList(entry));

    given()
        .header(AUTHENTICATION_HEADER)
        .body(clusterDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);

    final ClusterScriptFrom scriptFrom = entry.getScriptFrom();
    final SecretKeySelector secretKeyRef = scriptFrom.getSecretKeyRef();
    Secret secret = mockServer.getClient().secrets().inNamespace("test")
        .withName(secretKeyRef.getName())
        .get();
    assertNotNull(secret);

    byte[] actualScript = Base64.getDecoder().decode(secret.getData().get(secretKeyRef.getKey()));
    assertEquals(scriptFrom.getSecretScript(),
        new String(actualScript, StandardCharsets.UTF_8));
  }

  @Test
  void givenACreationWithSecretAndConfigMapScripts_shouldNotFail() {
    ClusterScriptEntry secretScriptEntry = getSecretScriptEntry();
    ClusterScriptEntry configMapScriptEntry = getConfigMapScriptEntry();

    clusterDto.getSpec().getInitData().setScripts(ImmutableList
        .of(secretScriptEntry, configMapScriptEntry));

    given()
        .header(AUTHENTICATION_HEADER)
        .body(clusterDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);

    final ClusterScriptFrom secretScriptFrom = secretScriptEntry.getScriptFrom();
    final SecretKeySelector secretKeyRef = secretScriptFrom.getSecretKeyRef();
    Secret secret = mockServer.getClient().secrets().inNamespace("test")
        .withName(secretKeyRef.getName())
        .get();
    assertNotNull(secret);

    byte[] actualScript = Base64.getDecoder().decode(secret.getData().get(secretKeyRef.getKey()));
    assertEquals(secretScriptFrom.getSecretScript(),
        new String(actualScript, StandardCharsets.UTF_8));

    final ClusterScriptFrom configMapScriptFrom = configMapScriptEntry.getScriptFrom();
    final ConfigMapKeySelector configMapKeyRef = configMapScriptFrom.getConfigMapKeyRef();
    ConfigMap configMap = mockServer.getClient().configMaps().inNamespace("test")
        .withName(configMapKeyRef.getName())
        .get();

    assertNotNull(configMap);

    assertEquals(configMapScriptFrom.getConfigMapScript(),
        configMap.getData().get(configMapKeyRef.getKey()));
  }

  private ClusterScriptEntry getSecretScriptEntry() {
    ClusterScriptEntry entry = new ClusterScriptEntry();
    entry.setName("init");
    final ClusterScriptFrom scriptFrom = new ClusterScriptFrom();
    scriptFrom.setSecretScript("CREATE DATABASE test");
    final SecretKeySelector secretMapKeyRef = new SecretKeySelector();
    scriptFrom.setSecretKeyRef(secretMapKeyRef);
    secretMapKeyRef.setKey("script");
    secretMapKeyRef.setName("initScript");
    entry.setScriptFrom(scriptFrom);
    return entry;
  }

  private ClusterScriptEntry getConfigMapScriptEntry() {
    ClusterScriptEntry entry = new ClusterScriptEntry();
    entry.setName("init");
    final ClusterScriptFrom scriptFrom = new ClusterScriptFrom();
    scriptFrom.setConfigMapScript("CREATE DATABASE test");
    final ConfigMapKeySelector configMapKeyRef = new ConfigMapKeySelector();
    scriptFrom.setConfigMapKeyRef(configMapKeyRef);
    configMapKeyRef.setKey("script");
    configMapKeyRef.setName("initScript");
    entry.setScriptFrom(scriptFrom);
    return entry;
  }

  @Test
  void givenACreationWithBackups_shouldNotFail() {
    clusterDto.getMetadata().setName(StringUtils.getRandomClusterName());
    ClusterSpec spec = clusterDto.getSpec();
    spec.setInitData(null);
    spec.setConfigurations(new ClusterConfiguration());
    spec.getConfigurations().setBackups(new ArrayList<>());

    ClusterBackupsConfiguration clusterBackupsConfiguration = new ClusterBackupsConfiguration();
    clusterBackupsConfiguration.setCompressionMethod("brotli");
    clusterBackupsConfiguration.setCronSchedule("100 8 10 10 10");
    clusterBackupsConfiguration.setRetention(10);
    clusterBackupsConfiguration.setObjectStorage("backupconf");
    clusterBackupsConfiguration.setPerformance(new BaseBackupPerformance());
    clusterBackupsConfiguration.getPerformance().setMaxDiskBandwidth(10L);
    clusterBackupsConfiguration.getPerformance().setMaxNetworkBandwidth(10L);
    clusterBackupsConfiguration.getPerformance().setUploadDiskConcurrency(10);

    spec.getConfigurations().getBackups().add(clusterBackupsConfiguration);

    given()
        .header(AUTHENTICATION_HEADER)
        .body(clusterDto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);

    given()
        .header(AUTHENTICATION_HEADER)
        .pathParam("namespace", clusterDto.getMetadata().getNamespace())
        .pathParam("name", clusterDto.getMetadata().getName())
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/namespaces/{namespace}/sgclusters/{name}")
        .then()
        .body("metadata.namespace", equalTo("test"),
            "metadata.name", equalTo(clusterDto.getMetadata().getName()),
            "spec.instances", equalTo(1),
            "spec.configurations.backups[0].compression", equalTo("brotli"),
            "spec.configurations.backups[0].cronSchedule", equalTo("100 8 10 10 10"),
            "spec.configurations.backups[0].retention", equalTo(10),
            "spec.configurations.backups[0].sgObjectStorage", equalTo("backupconf"),
            "spec.configurations.backups[0].performance.maxNetworkBandwidth", equalTo(10),
            "spec.configurations.backups[0].performance.maxDiskBandwidth", equalTo(10),
            "spec.configurations.backups[0].performance.uploadDiskConcurrency", equalTo(10))
        .statusCode(200);
  }

}
