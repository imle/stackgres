/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.AbstractRequiredResourceDecoratorTest;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.fixture.SecretFixture;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import io.stackgres.operator.fixture.StackGresDistributedLogsFixture;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class DistributedLogsRequireResourceDecoratorTest
    extends AbstractRequiredResourceDecoratorTest<StackGresDistributedLogsContext> {

  @Inject
  private DistributedLogsRequireResourceDecorator resourceDecorator;

  private StackGresDistributedLogs resource;

  private Optional<Secret> secret;

  private StackGresCluster connectecCluster;

  @BeforeEach
  public void setup() {
    this.resource = new StackGresDistributedLogsFixture().build("default");
    this.connectecCluster = new StackGresClusterFixture().build("default");
    this.secret = ofNullable(new SecretFixture().build("minio"));
  }

  @Override
  protected String usingCrdFilename() {
    return "SGDistributedLogs.yaml";
  }

  @Override
  protected HasMetadata getResource() {
    return this.resource;
  }

  @Override
  protected RequiredResourceDecorator<StackGresDistributedLogsContext> getResourceDecorator() {
    return resourceDecorator;
  }

  @Override
  protected StackGresDistributedLogsContext getResourceContext() throws IOException {
    return ImmutableStackGresDistributedLogsContext.builder()
        .source(resource)
        .addAllConnectedClusters(List.of(connectecCluster))
        .databaseCredentials(secret)
        .build();
  }

}