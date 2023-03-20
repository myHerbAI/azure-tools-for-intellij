/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.monitor.view.AzureMonitorView;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class AzureMonitorManager {
    public static final String AZURE_MONITOR_WINDOW = "Azure Monitor";
    public static final String AZURE_MONITOR_TRIGGERED = "AzureMonitor.Triggered";
    private static final Map<Project, ToolWindow> toolWindowMap = new HashMap<>();
    private static final Map<Project, AzureMonitorView> monitorTableViewMap = new HashMap<>();
    private static final Map<Project, AzureMonitorView> monitorQueryViewMap = new HashMap<>();
    private static final AzureMonitorManager instance = new AzureMonitorManager();
    public static AzureMonitorManager getInstance() {
        return instance;
    }

    @AzureOperation(name="user/monitor.open_azure_monitor")
    public void openMonitorWindow(@Nonnull Project project, @Nullable LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        final ToolWindow azureMonitorWindow = getToolWindow(project, workspace, resourceId);
        Optional.ofNullable(workspace).ifPresent(w -> AzureEventBus.emit("azure.monitor.change_workspace", w));
        Optional.ofNullable(azureMonitorWindow).ifPresent(it -> AzureTaskManager.getInstance().runLater(
                () -> it.activate(() -> {
                    it.setAvailable(true);
                    it.show();
                }))
        );
    }

    @Nullable
    private ToolWindow getToolWindow(@Nonnull Project project, @Nullable LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        if (toolWindowMap.containsKey(project)) {
            Optional.ofNullable(monitorTableViewMap.get(project)).ifPresent(t ->
                    Optional.ofNullable(workspace).ifPresent(w -> {
                        t.setSelectedWorkspace(workspace);
                        t.setInitResourceId(resourceId);
                    }));
            Optional.ofNullable(monitorQueryViewMap.get(project)).ifPresent(t ->
                    Optional.ofNullable(workspace).ifPresent(t::setSelectedWorkspace));
            return toolWindowMap.get(project);
        }
        return initToolWindow(project, workspace, resourceId);
    }

    @Nullable
    private ToolWindow initToolWindow(@Nonnull Project project, @Nullable LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        final ToolWindow azureMonitorWindow = ToolWindowManager.getInstance(project).getToolWindow(AZURE_MONITOR_WINDOW);
        if (Objects.isNull(azureMonitorWindow)) {
            return null;
        }
        if (Objects.isNull(azureMonitorWindow.getContentManager().findContent("Tables"))) {
            final AzureMonitorView monitorTableView = new AzureMonitorView(project, workspace, true, resourceId);
            this.addContent(azureMonitorWindow, "Tables", monitorTableView);
            monitorTableViewMap.put(project, monitorTableView);
        }
        if (Objects.isNull(azureMonitorWindow.getContentManager().findContent("Queries"))) {
            final AzureMonitorView monitorQueryView = new AzureMonitorView(project, workspace, false, resourceId);
            this.addContent(azureMonitorWindow, "Queries", monitorQueryView);
            monitorQueryViewMap.put(project, monitorQueryView);
        }
        toolWindowMap.put(project, azureMonitorWindow);
        return azureMonitorWindow;
    }

    private void addContent(@Nonnull ToolWindow toolWindow, String contentName, AzureMonitorView view) {
        final Content tableContent = ContentFactory.getInstance().createContent(view.getContentPanel(), contentName, true);
        tableContent.setCloseable(false);
        toolWindow.getContentManager().addContent(tableContent);
    }

    public static class AzureMonitorFactory implements ToolWindowFactory {
        private boolean isTriggered = false;
        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            if (!isTriggered) {
                isTriggered = PropertiesComponent.getInstance().getBoolean(AzureMonitorManager.AZURE_MONITOR_TRIGGERED);
            }
            toolWindow.setIcon(isTriggered ? IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE_MONITOR) : IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE_MONITOR_NEW));
            if (!toolWindowMap.containsKey(project)) {
                instance.initToolWindow(project, null, null);
            }
        }

        @Override
        public boolean shouldBeAvailable(@NotNull Project project) {
            return true;
        }

        @Nullable
        private LogAnalyticsWorkspace getDefaultWorkspace() {
            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                return null;
            }
            LogAnalyticsWorkspace defaultWorkspace = null;
            final Account account = Azure.az(AzureAccount.class).account();
            if (Objects.nonNull(account) && account.getSelectedSubscriptions().size() > 0) {
                final Subscription subscription = account.getSelectedSubscriptions().get(0);
                final List<LogAnalyticsWorkspace> workspaceList = Azure.az(AzureLogAnalyticsWorkspace.class)
                        .logAnalyticsWorkspaces(subscription.getId()).list().stream().toList();
                if (workspaceList.size() > 0) {
                    defaultWorkspace = workspaceList.get(0);
                }
            }
            return defaultWorkspace;
        }
    }

}
