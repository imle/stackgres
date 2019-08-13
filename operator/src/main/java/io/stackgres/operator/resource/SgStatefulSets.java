/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ResourceUtils;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.crd.pgconfig.StackGresPostgresConfig;
import io.stackgres.operator.crd.pgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.crd.pgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.crd.pgconfig.StackGresPostgresConfigList;
import io.stackgres.operator.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.crd.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.crd.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.crd.sgprofile.StackGresProfileList;
import io.stackgres.operator.parameters.Blacklist;
import io.stackgres.operator.patroni.PatroniConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgStatefulSets {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgStatefulSets.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  public StatefulSet create(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();
    final String pg_version = resource.getSpec().getPostgresVersion();
    final Optional<StackGresProfile> profile = getProfile(resource);

    Map<String, Quantity> request = null;
    Map<String, Quantity> storage = null;
    if (profile.isPresent()) {
      request = ImmutableMap.of("cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory()));
      storage = ImmutableMap.of("storage", new Quantity(profile.get().getSpec().getStorage()));
    }

    Map<String, String> labels = ResourceUtils.defaultLabels(name);

    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")

        .build();

    VolumeMount pgData = new VolumeMountBuilder()
        .withName("pg-data")
        .withMountPath("/var/lib/postgresql")
        .build();

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withReplicas(resource.getSpec().getInstances())
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(labels)
            .build())
        .withServiceName(name)
        .withTemplate(new PodTemplateSpecBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(labels)
                .build())
            .withNewSpec()
            .withShareProcessNamespace(Boolean.TRUE)
            .withServiceAccountName(name + SgPatroniRole.SUFIX)
            .addNewContainer()
            .withName("patroni")
            .withImage("docker.io/ongres/patroni:11.5")
            .withImagePullPolicy("Always")
            .withSecurityContext(new SecurityContextBuilder()
                .withRunAsUser(999L)
                .withAllowPrivilegeEscalation(Boolean.FALSE)
                .build())
            .withPorts(
                new ContainerPortBuilder().withContainerPort(5432).build(),
                new ContainerPortBuilder().withContainerPort(8008).build())
            .withVolumeMounts(pgSocket, pgData)
            .withEnvFrom(new EnvFromSourceBuilder()
                .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                    .withName(name).build())
                .build())
            .withEnv(
                new EnvVarBuilder().withName("PATRONI_NAME")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.name").build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_KUBERNETES_NAMESPACE")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.namespace")
                            .build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_KUBERNETES_POD_IP")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("status.podIP").build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_SUPERUSER_PASSWORD")
                    .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                        new SecretKeySelectorBuilder()
                            .withName(name)
                            .withKey("superuser-password")
                            .build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
                    .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                        new SecretKeySelectorBuilder()
                            .withName(name)
                            .withKey("replication-password")
                            .build())
                        .build())
                    .build())
            .withLivenessProbe(new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder()
                    .withPath("/health")
                    .withPort(new IntOrString(8008))
                    .withScheme("HTTP")
                    .build())
                .withInitialDelaySeconds(5)
                .withPeriodSeconds(30)
                .build())
            .withReadinessProbe(new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder()
                    .withPath("/health")
                    .withPort(new IntOrString(8008))
                    .withScheme("HTTP")
                    .build())
                .withInitialDelaySeconds(5)
                .withPeriodSeconds(30)
                .build())
            .withResources(new ResourceRequirementsBuilder()
                .withRequests(request)
                .build())
            .endContainer()
            .addNewContainer()
            .withName("postgres-util")
            .withImage("docker.io/ongres/postgres-util:11.5")
            .withImagePullPolicy("Always")
            .withNewSecurityContext()
            .withNewCapabilities()
            .addNewAdd("SYS_PTRACE")
            .endCapabilities()
            .endSecurityContext()
            .addNewEnv()
            .withName("PG_VERSION")
            .withValue(pg_version)
            .endEnv()
            .withStdin(Boolean.TRUE)
            .withTty(Boolean.TRUE)
            .withCommand("/bin/sh")
            .withArgs("-c", "while true; do sleep 10; done")
            .withVolumeMounts(pgSocket)
            .endContainer()
            .withVolumes(new VolumeBuilder()
                .withName("pg-socket")
                .withNewEmptyDir()
                .withMedium("Memory")
                .endEmptyDir()
                .build())
            .withTerminationGracePeriodSeconds(10L)
            .withInitContainers(new ContainerBuilder()
                .withName("data-permissions")
                .withImage("busybox")
                .withCommand("/bin/sh")
                .withArgs("-c", "chmod 755 /var/lib/postgresql "
                    + "&& chown 999:999 /var/lib/postgresql")
                .withVolumeMounts(pgData)
                .build())
            .endSpec()
            .build())
        .withVolumeClaimTemplates(new PersistentVolumeClaimBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName("pg-data")
                .withLabels(labels)
                .build())
            .withSpec(new PersistentVolumeClaimSpecBuilder()
                .withAccessModes("ReadWriteOnce")
                .withResources(new ResourceRequirementsBuilder()
                    .withRequests(storage)
                    .build())
                .build())
            .build())
        .endSpec()
        .build();

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      StatefulSet ss = client.apps().statefulSets().inNamespace(namespace).withName(name).get();
      if (ss == null) {
        statefulSet = client.apps().statefulSets().inNamespace(namespace).create(statefulSet);
        LOGGER.debug("Creating StatefulSet: {}", name);
      }
    }

    Optional<StackGresPostgresConfig> pgConfig = getPostgresConfig(resource);
    LOGGER.debug("StackGresPostgresConfig: {}", pgConfig);
    pgConfig.ifPresent(c -> applyPostgresConf(resource, c));

    LOGGER.trace("StatefulSet: {}", statefulSet);
    return statefulSet;
  }

  private void applyPostgresConf(StackGresCluster resource, StackGresPostgresConfig config) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      Endpoints endpoint = null;
      String conf = null;
      PatroniConfig patroniConf = new PatroniConfig();
      ObjectMapper mapper = new ObjectMapper();
      while (endpoint == null || conf == null) {
        endpoint = client.endpoints().inNamespace(namespace)
            .withName(name + SgServices.CONFIG_SERVICE).get();
        LOGGER.debug("Get config endpoint: {}", endpoint);
        if (endpoint != null) {
          conf = endpoint.getMetadata().getAnnotations().get("config");
          if (conf != null) {
            try {
              patroniConf = mapper.readValue(conf, PatroniConfig.class);
            } catch (IOException e) {
              LOGGER.error("IOException", e);
            }
          }
        }
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
          LOGGER.error("InterruptedException", e);
          Thread.currentThread().interrupt();
        }
      }

      LOGGER.debug("Configuration DCS: {}", conf);
      try {
        Map<String, String> newParams = config.getSpec().getPostgresqlConf();
        // Blacklist removal
        for (String bl : Blacklist.getBlacklistParameters()) {
          newParams.remove(bl);
        }

        PatroniConfig.PostgreSql postgres = patroniConf.getPostgresql();
        if (postgres == null) {
          postgres = new PatroniConfig.PostgreSql();
        }

        Map<String, String> params = postgres.getParameters();
        if (params == null) {
          params = new HashMap<>();
        }
        params.putAll(newParams);
        postgres.setParameters(params);

        endpoint.getMetadata().getAnnotations().put("config",
            mapper.writeValueAsString(patroniConf));
        endpoint = client.endpoints().inNamespace(namespace).createOrReplace(endpoint);

        LOGGER.debug("Modification EP {}", endpoint);

      } catch (IOException e) {
        LOGGER.error("IOException from Jackson on writing JSON", e);
      }
    }
  }

  private Optional<StackGresPostgresConfig> getPostgresConfig(StackGresCluster resource) {
    final String namespace = resource.getMetadata().getNamespace();
    final String pgConfig = resource.getSpec().getPostgresConfig();
    LOGGER.debug("PostgresConfig Name: {}", pgConfig);
    if (pgConfig != null) {
      try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
        return Optional.ofNullable(client
            .customResources(StackGresPostgresConfigDefinition.CR_DEFINITION,
                StackGresPostgresConfig.class,
                StackGresPostgresConfigList.class,
                StackGresPostgresConfigDoneable.class)
            .inNamespace(namespace)
            .withName(pgConfig)
            .get());
      }
    }
    return Optional.empty();
  }

  private Optional<StackGresProfile> getProfile(StackGresCluster resource) {
    final String namespace = resource.getMetadata().getNamespace();
    final String profileName = resource.getSpec().getResourceProfile();
    LOGGER.debug("StackGres Profile Name: {}", profileName);
    if (profileName != null) {
      try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
        return Optional.ofNullable(client
            .customResources(StackGresProfileDefinition.CR_DEFINITION,
                StackGresProfile.class,
                StackGresProfileList.class,
                StackGresProfileDoneable.class)
            .inNamespace(namespace)
            .withName(profileName)
            .get());
      }
    }
    return Optional.empty();
  }

  /**
   * Update the specification of the cluster.
   */
  public StatefulSet update(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      StatefulSet statefulSet =
          client.apps().statefulSets().inNamespace(namespace).withName(name).get();
      if (statefulSet != null) {
        int instances = resource.getSpec().getInstances();

        StatefulSetSpec spec = statefulSet.getSpec();
        if (spec.getReplicas() != instances) {
          spec.setReplicas(instances);
        }

        getPostgresConfig(resource).ifPresent(c -> applyPostgresConf(resource, c));

        statefulSet = client.apps().statefulSets().inNamespace(namespace)
            .createOrReplace(statefulSet);
      }

      LOGGER.debug("Updating StatefulSet: {}", name);
      return statefulSet;
    }
  }

  /**
   * Delete resource.
   */
  public Boolean delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public Boolean delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    Boolean deleted = client.apps().statefulSets().inNamespace(namespace)
        .withLabels(ResourceUtils.defaultLabels(name)).delete();

    LOGGER.debug("Deleting StatefulSet: {}, success: {}", name, deleted);
    return deleted;
  }

}
