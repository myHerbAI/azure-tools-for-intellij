/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.AnActionLink;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.MonitorTreePanel;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.WorkspaceSelectionDialog;
import com.microsoft.azure.toolkit.intellij.monitor.view.right.MonitorTabbedPane;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Optional;

public class AzureMonitorView {
    private JPanel contentPanel;
    private JPanel leftPanel;
    private ActionLink changeWorkspace;
    @Getter
    private MonitorTreePanel monitorTreePanel;
    private JLabel workspaceName;
    private JPanel workspaceHeader;
    private JPanel rightPanel;
    private MonitorTabbedPane tabbedPanePanel;
    @Getter
    @Nonnull
    private LogAnalyticsWorkspace selectedWorkspace;

    public AzureMonitorView(Project project, @Nonnull LogAnalyticsWorkspace logAnalyticsWorkspace, boolean isTableTab, @Nullable String resourceId) {
        this.selectedWorkspace = logAnalyticsWorkspace;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.workspaceName.setText(selectedWorkspace.getName());
        this.workspaceName.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        this.monitorTreePanel.setTableTab(isTableTab);
        this.tabbedPanePanel.setTableTab(isTableTab);
        this.tabbedPanePanel.setParentView(this);
        this.tabbedPanePanel.setInitResourceId(resourceId);
        AzureEventBus.on("azure.monitor.change_workspace", new AzureEventBus.EventListener(e -> {
            final LogAnalyticsWorkspace workspace = (LogAnalyticsWorkspace) e.getSource();
            this.selectedWorkspace = workspace;
            this.workspaceName.setText(workspace.getName());
        }));
        AzureTaskManager.getInstance().runInBackground(AzureString.fromString("Loading logs"), () -> this.monitorTreePanel.refresh());
    }

    public String getQueryString(String queryName) {
        return String.format("%s | take %s", this.getMonitorTreePanel().getQueryString(queryName), Azure.az().config().getMonitorQueryRowNumber());
    }

    public void setSelectedWorkspace(LogAnalyticsWorkspace workspace) {
        this.selectedWorkspace = workspace;
        this.workspaceName.setText(workspace.getName());
    }

    public void setInitResourceId(String resourceId) {
        this.tabbedPanePanel.setInitResourceId(resourceId);
        tabbedPanePanel.selectTab("AppTraces");
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        this.changeWorkspace = new AnActionLink("Change", new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                AzureTaskManager.getInstance().runLater(() -> {
                    final WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(e.getProject(), selectedWorkspace);
                    if (dialog.showAndGet()) {
                        Optional.ofNullable(dialog.getWorkspace()).ifPresent(w -> {
                            AzureEventBus.emit("azure.monitor.change_workspace", w);
                        });
                    }
                });
            }
        });
        this.monitorTreePanel = new MonitorTreePanel();
        this.tabbedPanePanel = new MonitorTabbedPane();
        this.rightPanel = tabbedPanePanel.getContentPanel();
    }

}
