/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.storages;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class GoogleCloudStorage {

  @JsonProperty("prefix")
  @NotNull(message = "The prefix is required")
  private String prefix;

  @JsonProperty("credentials")
  @NotNull(message = "The credentials is required")
  private GoogleCloudCredentials credentials;

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public GoogleCloudCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(GoogleCloudCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentials, prefix);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof GoogleCloudStorage)) {
      return false;
    }
    GoogleCloudStorage other = (GoogleCloudStorage) obj;
    return Objects.equals(credentials, other.credentials) && Objects.equals(prefix, other.prefix);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("prefix", prefix)
        .add("credentials", credentials)
        .toString();
  }

}