/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

public interface OperatorWatcherHandler {
  void startWatchers();

  void stopWatchers();
}
