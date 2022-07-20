/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpecScheduling;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.ImmutableStackGresDbOpsContext;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class DbOpsJobTestCase {

  @Inject
  @OperatorVersionBinder
  DbOpsJobsGenerator dbOpsJobsGenerator;

  StackGresCluster cluster;

  StackGresDbOps dbOps;

  StackGresProfile clusterProfile;

  @BeforeEach
  void setUp() {
    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);
    dbOps = JsonUtil.readFromJson(fixturePath(), StackGresDbOps.class);
    clusterProfile = JsonUtil.readFromJson("stackgres_profiles/size-xs.json",
        StackGresProfile.class);
  }

  abstract String fixturePath();

  void setSgDbOpsScheduling() {
    var dbopsScheduling = JsonUtil.readFromJson("stackgres_dbops/dbops_scheduling.json",
        StackGresDbOpsSpecScheduling.class);
    dbOps.getSpec().setScheduling(dbopsScheduling);
  }

  @Test
  void givenAContextWithASingleDbOpsWithoutRunAt_itShouldGenerateAJob() {
    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .profile(clusterProfile)
        .build();

    dbOps.getSpec().setRunAt(null);
    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.stream().filter(r -> r.getKind().equals("Job"))
        .count());
  }

  @Test
  void givenAContextWithADbOpsWithAPastRunAt_shouldGenerateAJob() {
    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .profile(clusterProfile)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().minusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1, generatedResources.size());
  }

  @Test
  void givenAContextWithADbOpsWithAFutureRunAt_shouldNotGenerateAJob() {
    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .profile(clusterProfile)
        .build();

    dbOps.getSpec().setRunAt(Instant.now().plusMillis(1000).toString());

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0, generatedResources.size());
  }

  @Test
  void shouldGenerateJobWithNodeSelector_onceSgDbOpsSchedulingHasNodeSelector() {
    dbOps.getSpec().setRunAt(null);
    setSgDbOpsScheduling();

    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .profile(clusterProfile)
        .build();

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());
    var job = (Job) generatedResources.iterator().next();
    assertEquals(2, job.getSpec().getTemplate().getSpec().getNodeSelector().size());

  }

  @Test
  void shouldGenerateJobWithNodeAffinity_onceSgDbOpsSchedulingHasAffinity() {
    dbOps.getSpec().setRunAt(null);
    setSgDbOpsScheduling();

    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(dbOps)
        .cluster(cluster)
        .profile(clusterProfile)
        .build();

    var generatedResources = dbOpsJobsGenerator.generateResource(context)
        .collect(Collectors.toUnmodifiableList());

    var job = (Job) generatedResources.iterator().next();
    var nodeAffinity = job.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity();
    assertEquals(1, nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size());
    assertEquals(1, nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()
        .getNodeSelectorTerms().size());
  }

}