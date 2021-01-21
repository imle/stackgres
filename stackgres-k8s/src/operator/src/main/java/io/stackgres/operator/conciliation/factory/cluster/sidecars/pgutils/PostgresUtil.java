/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import javax.inject.Singleton;

import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.RunningContainer;

@Sidecar(StackgresClusterContainers.POSTGRES_UTIL)
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@RunningContainer(order = 5)
public class PostgresUtil extends AbstractPostgresUtil {

}
