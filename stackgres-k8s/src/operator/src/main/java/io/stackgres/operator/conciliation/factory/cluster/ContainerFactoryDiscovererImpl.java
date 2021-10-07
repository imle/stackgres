/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;

@ApplicationScoped
public class ContainerFactoryDiscovererImpl
    extends ResourceDiscoverer<ContainerFactory<StackGresClusterContainerContext>>
    implements ContainerFactoryDiscoverer<StackGresClusterContainerContext> {

  @Inject
  public ContainerFactoryDiscovererImpl(
      @RunningContainer
          Instance<ContainerFactory<StackGresClusterContainerContext>> instance) {
    init(instance);
    resourceHub.forEach((key, value) -> {
      value.sort((f1, f2) -> {
        int f1Order = f1.getClass().getAnnotation(RunningContainer.class)
            .value().ordinal();
        int f2Order = f2.getClass().getAnnotation(RunningContainer.class)
            .value().ordinal();
        return Integer.compare(f1Order, f2Order);
      });
    });
  }

  @Override
  public List<ContainerFactory<StackGresClusterContainerContext>> discoverContainers(
      StackGresClusterContainerContext context) {
    return resourceHub.get(context.getClusterContext().getVersion()).stream()
        .filter(f -> f.isActivated(context))
        .collect(Collectors.toUnmodifiableList());
  }
}
