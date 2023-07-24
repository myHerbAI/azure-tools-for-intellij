/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.microsoft.azure.toolkit.intellij.monitor.view.AzureMonitorView;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MonitorTabbedPane implements Disposable {
    private final AzureEventBus.EventListener onWorkspaceChanged;
    @Getter
    private JPanel contentPanel;
    private JTabbedPane closeableTabbedPane;
    @Setter
    private boolean isTableTab;
    private AzureMonitorView parentView;
    private final List<String> openedTabs = new ArrayList<>();
    @Setter
    private String initResourceId;

    public MonitorTabbedPane() {
        this.onWorkspaceChanged = new AzureEventBus.EventListener(e -> reloadSelectedTab());
        AzureEventBus.on("azure.monitor.change_workspace", onWorkspaceChanged);
    }

    public void setParentView(AzureMonitorView parentView) {
        this.parentView = parentView;
        this.parentView.getMonitorTreePanel().addTreeSelectionListener(e -> Optional.ofNullable(e.getNewLeadSelectionPath())
            .ifPresent(it -> {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) it.getLastPathComponent();
                if (Objects.nonNull(node) && node.isLeaf()) {
                    selectTabTriggeredByUser(node.toString());
                }
            }));
    }

    public void selectTab(String tabName) {
        if (Objects.nonNull(initResourceId)) {
            final int removeIndex = closeableTabbedPane.indexOfTab(tabName);
            if (removeIndex != -1) {
                closeableTabbedPane.remove(removeIndex);
                openedTabs.remove(tabName);
            }
        }
        if (!openedTabs.contains(tabName)) {
            final MonitorSingleTab singleTab = new MonitorSingleTab(this.isTableTab, tabName, this.parentView, initResourceId);
            this.closeableTabbedPane.addTab(tabName, singleTab.getSplitter());
            this.closeableTabbedPane.setTabComponentAt(this.closeableTabbedPane.getTabCount() - 1, createTabTitle(tabName));
            this.openedTabs.add(tabName);
        }
        final int toSelectIndex = this.closeableTabbedPane.indexOfTab(tabName);
        this.closeableTabbedPane.setSelectedIndex(toSelectIndex);
        this.initResourceId = null;
    }

    @AzureOperation(name = "user/monitor.change_table_tab", params = {"tabName"})
    private void selectTabTriggeredByUser(String tabName) {
        OperationContext.current().setTelemetryProperty("tabName", tabName);
        selectTab(tabName);
    }

    private void reloadSelectedTab() {
        this.initResourceId = null;
        this.openedTabs.clear();
        AzureTaskManager.getInstance().runLater(() -> {
            final int selectedIndex = this.closeableTabbedPane.getSelectedIndex();
            if (selectedIndex != -1 && selectedIndex < this.closeableTabbedPane.getTabCount()) {
                final String selectedTabName = this.closeableTabbedPane.getTitleAt(selectedIndex);
                this.closeableTabbedPane.removeAll();
                this.selectTab(selectedTabName);
            }
        }, AzureTask.Modality.ANY);
    }

    private JPanel createTabTitle(String tabName) {
        final JPanel tabTitle = new JPanel(new GridBagLayout());
        tabTitle.setOpaque(false);
        final JLabel iconLabel = new JLabel(AllIcons.Actions.Close);
        final JLabel textLabel = new JLabel(tabName);
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        tabTitle.add(textLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        tabTitle.add(iconLabel, gbc);
        iconLabel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final int i = closeableTabbedPane.indexOfTabComponent(tabTitle);
                if (i != -1) {
                    closeableTabbedPane.remove(i);
                    openedTabs.remove(tabName);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                iconLabel.setIcon(AllIcons.Actions.CloseHovered);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                iconLabel.setIcon(AllIcons.Actions.Close);
            }
        });
        return tabTitle;
    }

    @Override
    public void dispose() {
        AzureEventBus.off("azure.monitor.change_workspace", onWorkspaceChanged);
    }
}
