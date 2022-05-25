/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

@ApplicationScoped
@ProviderName(VolumeMountProviderName.HUGE_PAGES)
public class HugePagesMounts implements VolumeMountsProvider<StackGresClusterContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return Stream.concat(
        Optional.of(context.getClusterContext().getStackGresProfile().getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(PatroniStaticVolume.HUGEPAGES_2M.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.HUGEPAGES_2M_PATH.path())
                .build())
            .stream(),
        Optional.of(context.getClusterContext().getStackGresProfile().getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(PatroniStaticVolume.HUGEPAGES_1G.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.HUGEPAGES_1G_PATH.path())
                .build())
            .stream())
        .toList();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(StackGresClusterContainerContext context) {
    return List.of(
        ClusterStatefulSetPath.HUGEPAGES_2M_PATH.envVar(),
        ClusterStatefulSetPath.HUGEPAGES_1G_PATH.envVar()
    );
  }
}