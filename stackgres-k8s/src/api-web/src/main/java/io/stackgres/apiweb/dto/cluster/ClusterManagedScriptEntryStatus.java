/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterManagedScriptEntryStatus {

  @JsonProperty("id")
  private Integer id;

  @JsonProperty("startedAt")
  private String startedAt;

  @JsonProperty("updatedAt")
  private String updatedAt;

  @JsonProperty("failedAt")
  private String failedAt;

  @JsonProperty("completedAt")
  private String completedAt;

  @JsonProperty("scripts")
  private List<ClusterManagedScriptEntryScriptsStatus> scripts;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getFailedAt() {
    return failedAt;
  }

  public void setFailedAt(String failedAt) {
    this.failedAt = failedAt;
  }

  public String getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(String completedAt) {
    this.completedAt = completedAt;
  }

  public List<ClusterManagedScriptEntryScriptsStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<ClusterManagedScriptEntryScriptsStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(completedAt, failedAt, id, scripts, startedAt, updatedAt);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterManagedScriptEntryStatus)) {
      return false;
    }
    ClusterManagedScriptEntryStatus other = (ClusterManagedScriptEntryStatus) obj;
    return Objects.equals(completedAt, other.completedAt)
        && Objects.equals(failedAt, other.failedAt) && Objects.equals(id, other.id)
        && Objects.equals(scripts, other.scripts) && Objects.equals(startedAt, other.startedAt)
        && Objects.equals(updatedAt, other.updatedAt);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
