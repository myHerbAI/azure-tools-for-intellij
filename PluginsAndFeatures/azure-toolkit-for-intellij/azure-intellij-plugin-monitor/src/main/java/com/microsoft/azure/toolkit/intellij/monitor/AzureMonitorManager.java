/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.intellij.monitor.view.AzureMonitorView;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;


public class AzureMonitorManager {
    public static final String AZURE_MONITOR_WINDOW = "Azure Monitor";
    private static final Map<Project, ToolWindow> toolWindowMap = new HashMap<>();
    private static final Map<Project, AzureMonitorView> monitorViewMap = new HashMap<>();
    private static final AzureMonitorManager instance = new AzureMonitorManager();
    public static AzureMonitorManager getInstance() {
        return instance;
    }

    @AzureOperation(name="user/monitor.open_azure_monitor")
    public void openMonitorWindow(@Nonnull Project project, @Nonnull LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        final ToolWindow azureMonitorWindow = getToolWindow(project, workspace, resourceId);
        Optional.ofNullable(azureMonitorWindow).ifPresent(it -> AzureTaskManager.getInstance().runLater(
                () -> it.activate(() -> {
                    it.setAvailable(true);
                    it.show();
                }))
        );
    }

    @Nullable
    private ToolWindow getToolWindow(@Nonnull Project project, @Nonnull LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        if (toolWindowMap.containsKey(project)) {
            final AzureMonitorView tableView = monitorViewMap.get(project);
            if (Objects.nonNull(tableView)) {
                tableView.setSelectedWorkspace(workspace);
                tableView.setInitResourceId(resourceId);
            }
            return toolWindowMap.get(project);
        }
        final ToolWindow azureMonitorWindow = ToolWindowManager.getInstance(project).getToolWindow(AZURE_MONITOR_WINDOW);
        if (Objects.isNull(azureMonitorWindow)) {
            return null;
        }
        final ContentFactory contentFactory = ContentFactory.getInstance();
        final AzureMonitorView monitorTableView = new AzureMonitorView(project, workspace, true, resourceId);
        final AzureMonitorView monitorQueryView = new AzureMonitorView(project, workspace, false, resourceId);
        final Content tableContent = contentFactory.createContent(monitorTableView.getContentPanel(), "Tables", true);
        tableContent.setCloseable(false);
        final Content queryContent = contentFactory.createContent(monitorQueryView.getContentPanel(), "Queries", true);
        queryContent.setCloseable(false);
        azureMonitorWindow.getContentManager().addContent(tableContent);
        azureMonitorWindow.getContentManager().addContent(queryContent);
        toolWindowMap.put(project, azureMonitorWindow);
        monitorViewMap.put(project, monitorTableView);
        return azureMonitorWindow;
    }

    public static class AzureMonitorFactory implements ToolWindowFactory {
        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        }

        @Override
        public boolean shouldBeAvailable(@NotNull Project project) {
            return false;
        }
    }

}
