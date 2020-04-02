/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pooling;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetPath;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfig;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigDefinition;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigDoneable;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigList;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigPgBouncer;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigSpec;
import io.stackgres.operator.sidecars.pooling.parameters.Blacklist;
import io.stackgres.operator.sidecars.pooling.parameters.DefaultValues;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Sidecar("connection-pooling")
@Singleton
public class PgPooling
    implements StackGresClusterSidecarResourceFactory<StackGresPoolingConfig> {

  private static final String NAME = "pgbouncer";
  private static final String IMAGE_PREFIX = "docker.io/ongres/pgbouncer:v%s-build-%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("pgbouncer");
  private static final String CONFIG_SUFFIX = "-connection-pooling-config";

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    String namespace = context.getClusterContext().getCluster().getMetadata().getNamespace();
    String configMapName = configName(context.getClusterContext());
    Optional<StackGresPoolingConfig> pgbouncerConfig =
        context.getClusterContext().getSidecarConfig(this);
    Map<String, String> newParams = pgbouncerConfig.map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerConf)
        .orElseGet(HashMap::new);
    // Blacklist removal
    for (String bl : Blacklist.getBlacklistParameters()) {
      newParams.remove(bl);
    }
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    for (Map.Entry<String, String> entry : newParams.entrySet()) {
      params.put(entry.getKey(), entry.getValue());
    }

    String configFile = "[databases]\n"
        + " * = port = " + Envoy.PG_RAW_PORT + "\n"
        + "\n"
        + "[pgbouncer]\n"
        + "listen_port = " + Envoy.PG_PORT + "\n"
        + "listen_addr = 127.0.0.1\n"
        + "unix_socket_dir = " + ClusterStatefulSetPath.PG_RUN_PATH.path() + "\n"
        + "auth_type = md5\n"
        + "auth_user = authenticator\n"
        + "auth_query = SELECT usename, passwd FROM pg_shadow WHERE usename=$1\n"
        + "admin_users = postgres\n"
        + "stats_users = postgres\n"
        + "user = postgres\n"
        + "application_name_add_host = 1\n"
        + "ignore_startup_parameters = extra_float_digits\n"
        + "max_db_connections = 100\n"
        + "max_user_connections = 100\n"
        + "default_pool_size = 100\n"
        + params.entrySet().stream()
        .map(entry -> " " + entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"))
        + "\n";
    Map<String, String> data = ImmutableMap.of("pgbouncer.ini", configFile);

    ConfigMap cm = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(StackGresUtil.clusterLabels(context.getClusterContext().getCluster()))
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
            context.getClusterContext().getCluster())))
        .endMetadata()
        .withData(data)
        .build();

    return Seq.of(cm);
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_PREFIX,
            DEFAULT_VERSION, StackGresUtil.CONTAINER_BUILD))
        .withImagePullPolicy("Always")
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.SOCKET
                .volumeMount(context.getClusterContext()),
            new VolumeMountBuilder()
                .withName(NAME)
                .withMountPath("/etc/pgbouncer")
                .withReadOnly(Boolean.TRUE)
                .build());

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(
      StackGresGeneratorContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context.getClusterContext()))
            .build())
        .build());
  }

  @Override
  public Optional<StackGresPoolingConfig> getConfig(StackGresCluster cluster,
                                                    KubernetesClient client) throws Exception {
    final String namespace = cluster.getMetadata().getNamespace();
    final String pgbouncerConfig = cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig();
    if (pgbouncerConfig != null) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresPoolingConfigDefinition.NAME);
      if (crd.isPresent()) {
        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresPoolingConfig.class,
                StackGresPoolingConfigList.class,
                StackGresPoolingConfigDoneable.class)
            .inNamespace(namespace)
            .withName(pgbouncerConfig)
            .get());
      }
    }
    return Optional.empty();
  }

}