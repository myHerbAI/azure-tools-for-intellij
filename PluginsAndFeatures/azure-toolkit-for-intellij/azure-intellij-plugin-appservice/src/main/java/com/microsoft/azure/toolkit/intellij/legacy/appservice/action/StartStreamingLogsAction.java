/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceStreamingLogManager;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StartStreamingLogsAction {
    private final Project project;
    private final AppServiceAppBase<?, ?, ?> appService;
    private final String resourceId;

    public StartStreamingLogsAction(@Nonnull final AppServiceAppBase<?, ?, ?> appService, @Nullable final Project project) {
        super();
        this.project = project;
        this.appService = appService;
        this.resourceId = appService.getId();
    }

    public void execute() {
        final AzureString title = OperationBundle.description("appservice.open_log_stream.app", appService.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            if (appService instanceof WebApp) {
                AppServiceStreamingLogManager.INSTANCE.showWebAppStreamingLog(project, resourceId);
            } else if (appService instanceof WebAppDeploymentSlot) {
                AppServiceStreamingLogManager.INSTANCE.showWebAppDeploymentSlotStreamingLog(project, resourceId);
            } else if (appService instanceof FunctionApp) {
                AppServiceStreamingLogManager.INSTANCE.showFunctionStreamingLog(project, (FunctionApp) appService);
            } else if (appService instanceof FunctionAppDeploymentSlot) {
                AppServiceStreamingLogManager.INSTANCE.showFunctionDeploymentSlotStreamingLog(project, (FunctionAppDeploymentSlot) appService);
            } else {
                AzureMessager.getMessager().error("Unsupported operation", "Unsupported operation");
            }
        }));
    }
}
