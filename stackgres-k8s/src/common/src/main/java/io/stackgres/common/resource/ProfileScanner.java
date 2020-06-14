/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.ArcUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.crd.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;

@ApplicationScoped
public class ProfileScanner
    extends AbstractCustomResourceScanner<StackGresProfile, StackGresProfileList,
    StackGresProfileDoneable> {

  @Inject
  public ProfileScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresProfileDefinition.NAME,
        StackGresProfile.class, StackGresProfileList.class,
        StackGresProfileDoneable.class);
  }

  public ProfileScanner() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}