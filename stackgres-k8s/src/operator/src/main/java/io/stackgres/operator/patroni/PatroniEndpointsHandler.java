/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;

@ApplicationScoped
public class PatroniEndpointsHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof Endpoints
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && (resource.getMetadata().getName().equals(
            context.getCluster().getMetadata().getName())
        || resource.getMetadata().getName().equals(
            PatroniServices.readWriteName(context))
        || resource.getMetadata().getName().equals(
            PatroniServices.readOnlyName(context))
        || resource.getMetadata().getName().equals(
            PatroniServices.failoverName(context)));
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}