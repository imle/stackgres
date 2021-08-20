/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliationUtil;

@ApplicationScoped
public class BackupConciliator extends Conciliator<StackGresBackup> {

  @Override
  public ReconciliationResult evalReconciliationState(StackGresBackup config) {
    final ReconciliationResult reconciliationResult = super.evalReconciliationState(config);

    reconciliationResult.setDeletions(reconciliationResult.getDeletions().stream()
        .filter(ReconciliationUtil::isResourceReconciliationNotPausedUntilRestart)
        .collect(Collectors.toUnmodifiableList()));

    reconciliationResult.setPatches(reconciliationResult.getPatches().stream()
        .filter(tuple -> ReconciliationUtil
            .isResourceReconciliationNotPausedUntilRestart(tuple.v2))
        .collect(Collectors.toUnmodifiableList()));

    return reconciliationResult;
  }
}
