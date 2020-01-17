/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factories;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operatorframework.factories.ResourceRequirementsFactory;

@ApplicationScoped
public class ClusterStatefulSetPodRequirements
    implements ResourceRequirementsFactory<StackGresClusterContext> {

  @Override
  public ResourceRequirements getPodRequirements(StackGresClusterContext config) {
    final Optional<StackGresProfile> profile = config.getProfile();

    ResourceRequirements podResources = new ResourceRequirements();
    if (profile.isPresent()) {
      podResources.setRequests(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
      podResources.setLimits(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
    }
    return podResources;
  }
}