/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigAuthentication {

  @ValidEnum(enumClass = StackGresAuthenticationType.class, allowNulls = true,
      message = "type must be jwt or oidc")
  private String type;

  private StackGresConfigAuthenticationOidc oidc;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public StackGresConfigAuthenticationOidc getOidc() {
    return oidc;
  }

  public void setOidc(StackGresConfigAuthenticationOidc oidc) {
    this.oidc = oidc;
  }

  @Override
  public int hashCode() {
    return Objects.hash(oidc, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigAuthentication)) {
      return false;
    }
    StackGresConfigAuthentication other = (StackGresConfigAuthentication) obj;
    return Objects.equals(oidc, other.oidc) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
