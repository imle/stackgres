/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface ClusterMutator extends JsonPatchMutator<StackgresClusterReview> {

  JsonPointer CLUSTER_CONFIG_POINTER = JsonPointer.of("spec");

  default JsonPointer getTargetPointer(String field) throws NoSuchFieldException {
    String jsonField = getJsonMappingField(field, StackGresClusterSpec.class);
    return CLUSTER_CONFIG_POINTER.append(jsonField);
  }

  default String getJsonMappingField(String field, Class<?> clazz) throws NoSuchFieldException {
    return clazz.getDeclaredField(field)
        .getAnnotation(JsonProperty.class)
        .value();
  }

}
