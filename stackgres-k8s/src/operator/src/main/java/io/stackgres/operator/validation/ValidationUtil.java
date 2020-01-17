/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

public interface ValidationUtil {

  String VALIDATION_PATH = "/stackgres/validation";
  String CLUSTER_VALIDATION_PATH = VALIDATION_PATH + "/sgcluster";
  String PGCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgpgconfig";
  String CONNPOOLCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgconnectionpoolingconfig";
  String BACKUPCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgbackupconfig";
  String BACKUP_VALIDATION_PATH = VALIDATION_PATH + "/sgbackup";
  String PROFILE_VALIDATION_PATH = VALIDATION_PATH + "/sgprofile";
  String RESTORECONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgrestoreconfig";

}