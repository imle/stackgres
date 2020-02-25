/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassList;
import io.stackgres.operator.resource.KubernetesResourceScanner;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageClassResourceTest {

  @Mock
  private KubernetesResourceScanner<StorageClass> scanner;

  private StorageClassList storageClasses;

  private StorageClassResource resource;

  @BeforeEach
  void setUp() {
    storageClasses = JsonUtil
        .readFromJson("storage_class/list.json", StorageClassList.class);

    resource = new StorageClassResource(scanner);
  }

  @Test
  void getShouldReturnAllStorageClassesNames() {

    when(scanner.findResources()).thenReturn(storageClasses.getItems());

    List<String> storageClasses = resource.get();

    assertEquals(1, storageClasses.size());

    assertEquals("standard", storageClasses.get(0));

  }
}