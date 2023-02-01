/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.task;

import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.monitor.AzureMonitorManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class OpenLogsInMonitorAction {
    private final ApplicationInsight applicationInsight;
    private final String resourceId;
    private final Project project;
    public OpenLogsInMonitorAction(@Nonnull ApplicationInsight insight, @Nullable Project project) {
        this.applicationInsight = insight;
        this.project = project;
        this.resourceId = insight.getId();
    }

    public void execute() {
        Optional.ofNullable(getWorkspace()).ifPresent(it -> AzureTaskManager.getInstance().runLater(() ->
                AzureMonitorManager.getInstance().openMonitorWindow(project, it, resourceId)));
    }

    @Nullable
    private LogAnalyticsWorkspace getWorkspace() {
        final String workspaceResourceId = Optional.ofNullable(applicationInsight.getRemote())
                .map(ApplicationInsightsComponent::workspaceResourceId).orElse(StringUtils.EMPTY);
        if (StringUtils.isBlank(workspaceResourceId)) {
            AzureMessager.getMessager().info(message("azure.monitor.info.workspaceNotFoundInAI", applicationInsight.getName()));
            return null;
        }
        return Azure.az(AzureLogAnalyticsWorkspace.class).getById(workspaceResourceId);
    }
}
