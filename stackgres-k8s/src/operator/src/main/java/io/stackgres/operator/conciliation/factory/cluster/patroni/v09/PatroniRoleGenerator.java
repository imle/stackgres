/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v09;

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class PatroniRoleGenerator implements
    ResourceGenerator<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String roleName(ClusterContext clusterContext) {
    return PatroniUtil.roleName(clusterContext.getCluster());
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream.of(
        createServiceAccount(context),
        createRole(context),
        createRoleBinding(context));
  }

  /**
   * Create the ServiceAccount for patroni associated to the cluster.
   */
  private ServiceAccount createServiceAccount(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    final String serviceAccountName = roleName(context);
    final String serviceAccountNamespace = cluster.getMetadata().getNamespace();

    return new ServiceAccountBuilder()
        .withNewMetadata()
        .withName(serviceAccountName)
        .withNamespace(serviceAccountNamespace)
        .withLabels(labels)
        .endMetadata()
        .build();

  }

  /**
   * Create the Role for patroni associated to the cluster.
   */
  private Role createRole(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("endpoints", "configmaps")
            .withVerbs("create", "get", "list", "patch", "update", "watch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("secrets")
            .withVerbs("get")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods")
            .withVerbs("get", "list", "patch", "update", "watch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods/exec")
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("batch")
            .withResources("cronjobs")
            .withVerbs("get", "patch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("services")
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresBackup.class))
            .withVerbs("list", "get", "create", "patch", "delete")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(
                HasMetadata.getPlural(StackGresBackupConfig.class))
            .withVerbs("get")
            .build())
        .build();
  }

  /**
   * Create the RoleBinding for patroni associated to the cluster.
   */
  private RoleBinding createRoleBinding(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(roleName(context))
            .withNamespace(cluster.getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(roleName(context))
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
