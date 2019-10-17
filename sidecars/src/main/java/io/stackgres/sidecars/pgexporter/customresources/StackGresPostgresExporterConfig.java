/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter.customresources;

import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.client.CustomResource;

public class StackGresPostgresExporterConfig extends CustomResource {

  private static final long serialVersionUID = 2719099984653736636L;

  private StackGresPostgresExporterConfigSpec spec;

  public StackGresPostgresExporterConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresPostgresExporterConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("apiVersion", getApiVersion())
        .add("metadata", getMetadata())
        .add("spec", spec)
        .toString();
  }

}
