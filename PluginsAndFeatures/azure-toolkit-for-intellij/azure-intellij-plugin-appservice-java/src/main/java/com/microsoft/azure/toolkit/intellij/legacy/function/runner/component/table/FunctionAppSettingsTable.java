/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

public class FunctionAppSettingsTable extends AppSettingsTable {
    public static final String AZURE_WEB_JOB_STORAGE_KEY = "AzureWebJobsStorage";
    public static final String FUNCTIONS_WORKER_RUNTIME_KEY = "FUNCTIONS_WORKER_RUNTIME";
    public static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    public static final String AZURE_WEB_JOB_STORAGE_VALUE = "";
    @Getter
    @Setter
    @Nullable
    private String localSettingPath;
    @Getter
    @Setter
    private Project project;

    public FunctionAppSettingsTable() {
        this(null);
    }

    public FunctionAppSettingsTable(@Nullable String localSettingPath) {
        super();
        this.localSettingPath = localSettingPath;
    }

    public void loadLocalSetting() {
        if (StringUtils.isNotBlank(localSettingPath)) {
            final File localSettingsFile = new File(localSettingPath);
            if (localSettingsFile.exists()) {
                final Map<String, String> appSettings = FunctionAppSettingsTableUtils.getAppSettingsFromLocalSettingsJson(localSettingsFile);
                setAppSettings(appSettings);
            }
        }
    }

    public void loadRequiredSettings() {
        final Map<String, String> appSettingsMap = getAppSettings();
        if (!appSettingsMap.containsKey(FunctionAppSettingsTable.FUNCTIONS_WORKER_RUNTIME_KEY)) {
            this.addRequiredAppSettings(FunctionAppSettingsTable.FUNCTIONS_WORKER_RUNTIME_KEY, FunctionAppSettingsTable.FUNCTIONS_WORKER_RUNTIME_VALUE);
        }
        if (!appSettingsMap.containsKey(FunctionAppSettingsTable.AZURE_WEB_JOB_STORAGE_KEY)) {
            this.addRequiredAppSettings(FunctionAppSettingsTable.AZURE_WEB_JOB_STORAGE_KEY, FunctionAppSettingsTable.AZURE_WEB_JOB_STORAGE_VALUE);
        }
    }
}
