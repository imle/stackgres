/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.ClusterRestore;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterInitData;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultRestoreMutator implements ClusterMutator {

  protected static final ObjectMapper mapper = new ObjectMapper();

  private JsonPointer restorePointer;
  private JsonNode defaultNode;

  private DefaultCustomResourceFactory<ClusterRestore> defaultRestoreFactory;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String initDataJson = ClusterMutator.getJsonMappingField("initData",
        StackGresClusterSpec.class);

    String restoreJsonField = ClusterMutator.getJsonMappingField("restore",
        StackGresClusterInitData.class);

    restorePointer = ClusterMutator.CLUSTER_CONFIG_POINTER
        .append(initDataJson).append(restoreJsonField);

    ClusterRestore defaultRestore = defaultRestoreFactory.buildResource();
    defaultNode = mapper.valueToTree(defaultRestore);

  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {

    final StackGresClusterInitData initData = review.getRequest().getObject().getSpec()
        .getInitData();

    if (review.getRequest().getOperation() == Operation.CREATE
        && initData != null) {
      ClusterRestore restore = initData.getRestore();

      if (restore != null) {

        JsonNode target = mapper.valueToTree(restore);
        ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
        operations.addAll(applyDefaults(restorePointer, defaultNode, target));

        return operations.build();
      }

    }

    return ImmutableList.of();
  }

  @Inject
  public void setDefaultRestoreFactory(
      DefaultCustomResourceFactory<ClusterRestore> defaultRestoreFactory) {
    this.defaultRestoreFactory = defaultRestoreFactory;
  }
}
