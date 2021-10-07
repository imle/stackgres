/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtensionsValidatorTest {

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions().findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedBuildVersions().findFirst().get();

  private ExtensionsValidator validator;

  private List<StackGresClusterInstalledExtension> defaultExtensions;

  @Mock
  private ClusterExtensionMetadataManager extensionMetadataManager;

  @BeforeEach
  void setUp() {
    validator = new ExtensionsValidator(extensionMetadataManager);

    defaultExtensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getDefaultExtension)
        .collect(ImmutableList.toImmutableList());
  }

  private List<StackGresExtensionMetadata> getDefaultExtensionMetadatas(
      InvocationOnMock invocation) {
    return defaultExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((StackGresClusterExtension) invocation.getArgument(1)).getName()))
        .map(StackGresExtensionMetadata::new)
        .collect(ImmutableList.toImmutableList());
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(null);
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    validator.validate(review);
  }

  @Test
  void givenAnUpdate_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(null);
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    validator.validate(review);
  }

  @Test
  void givenACreationWithMissingExtensions_shouldFail() {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(null);
    when(extensionMetadataManager.getExtensionsAnyVersion(
        same(review.getRequest().getObject()), any()))
        .then(this::getDefaultExtensionMetadatas);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.EXTENSION_NOT_FOUND,
        "Some extensions were not found: dblink (available 1.0.0),"
            + " pg_stat_statements (available 1.0.0), plpgsql (available 1.0.0),"
            + " plpython3u (available 1.0.0)");
  }

  private StackGresClusterReview getCreationReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackGresClusterReview.class);
  }

  private StackGresClusterReview getUpdateReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/postgres_config_update.json",
            StackGresClusterReview.class);
  }

  private StackGresClusterInstalledExtension getDefaultExtension(String name) {
    final StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName(name);
    installedExtension.setPublisher("com.ongres");
    installedExtension.setRepository(OperatorProperty.EXTENSIONS_REPOSITORY_URLS.getString());
    installedExtension.setVersion("1.0.0");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    installedExtension.setBuild(BUILD_VERSION);
    return installedExtension;
  }

}
