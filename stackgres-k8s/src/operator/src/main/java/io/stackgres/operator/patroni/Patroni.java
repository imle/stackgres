/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.common.StackGresClusterConfigTransformer;

@ApplicationScoped
public class Patroni implements StackGresClusterConfigTransformer {

  @Inject
  ObjectMapperProvider objectMapperProvider;

  @Override
  public List<HasMetadata> getResources(StackGresClusterConfig config) {
    return ImmutableList.<HasMetadata>builder()
        .add(PatroniRole.createServiceAccount(config.getCluster()))
        .add(PatroniRole.createRole(config.getCluster()))
        .add(PatroniRole.createRoleBinding(config.getCluster()))
        .add(PatroniSecret.create(config.getCluster()))
        .addAll(PatroniServices.createServices(config.getCluster()))
        .add(PatroniEndpoints.create(config, objectMapperProvider.objectMapper()))
        .add(PatroniConfigMap.create(config, objectMapperProvider.objectMapper()))
        .addAll(StackGresStatefulSet.create(config))
        .build();
  }

  public boolean isManaged(StackGresClusterConfig config, HasMetadata sgResource) {
    return PatroniSecret.is(config.getCluster(), sgResource)
        || PatroniEndpoints.is(config.getCluster(), sgResource);
  }

  public boolean isStatefulSet(StackGresClusterConfig config, HasMetadata sgResource) {
    return StackGresStatefulSet.is(config.getCluster(), sgResource);
  }

  public boolean isEndpoints(StackGresClusterConfig config, HasMetadata sgResource) {
    return PatroniEndpoints.is(config.getCluster(), sgResource);
  }

  /**
   * Perform update of a component.
   */
  public void update(StackGresClusterConfig config, HasMetadata sgResource,
      KubernetesClient client) {
    if (isEndpoints(config, sgResource)) {
      Endpoints endpoints = (Endpoints) sgResource;
      Endpoints editedEndpoints = client.endpoints()
          .inNamespace(sgResource.getMetadata().getNamespace())
          .withName(sgResource.getMetadata().getName())
          .edit()
          .editMetadata()
          .addToAnnotations(PatroniEndpoints.PATRONI_CONFIG_KEY,
              endpoints.getMetadata().getAnnotations()
              .get(PatroniEndpoints.PATRONI_CONFIG_KEY))
          .endMetadata()
          .done();
      if (editedEndpoints == null) {
        client.resource(sgResource).createOrReplace();
      }
    }
    if (isStatefulSet(config, sgResource)) {
      StatefulSet statefulSet = (StatefulSet) sgResource;
      StatefulSet editedStatefulSet = client.apps().statefulSets()
          .inNamespace(sgResource.getMetadata().getNamespace())
          .withName(sgResource.getMetadata().getName())
          .scale(statefulSet.getSpec().getReplicas(), false);
      if (editedStatefulSet == null) {
        client.resource(sgResource).createOrReplace();
      }
    }
  }

}