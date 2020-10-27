/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.dto.configmap.ConfigMapDto;
import io.stackgres.apiweb.transformer.ConfigMapMapper;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.resource.ResourceScanner;

@ApplicationScoped
public class ConfigMapDtoScanner implements ResourceScanner<ConfigMapDto> {

  private final KubernetesClientFactory factory;

  @Inject
  public ConfigMapDtoScanner(KubernetesClientFactory factory) {
    this.factory = factory;
  }

  @Override
  public List<ConfigMapDto> findResources() {
    try (KubernetesClient client = factory.create()) {
      return client.configMaps().list().getItems().stream()
          .map(ConfigMapMapper::map)
          .collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public List<ConfigMapDto> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = factory.create()) {
      return client.configMaps().inNamespace(namespace).list().getItems().stream()
          .map(ConfigMapMapper::map)
          .collect(ImmutableList.toImmutableList());
    }
  }
}
