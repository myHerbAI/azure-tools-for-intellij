/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppConfigPanel;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstancesPanel;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Optional;

public class SpringCloudAppPropertiesEditor extends AzResourcePropertiesEditor<SpringCloudApp> {
    private JButton refreshButton;
    private JButton startButton;
    private JButton stopButton;
    private JButton restartButton;
    private JButton deleteButton;
    private JPanel contentPanel;
    private JButton saveButton;
    private ActionLink resetButton;
    private JBLabel lblSubscription;
    private JBLabel lblCluster;
    private JBLabel lblApp;
    private SpringCloudAppConfigPanel formConfig;
    private SpringCloudAppInstancesPanel panelInstances;

    @Nonnull
    private final Project project;
    @Nonnull
    private final SpringCloudApp app;
    @Nonnull
    private final SpringCloudAppDraft draft;
    private SpringCloudDeploymentDraft deploymentDraft;

    public SpringCloudAppPropertiesEditor(@Nonnull Project project, @Nonnull SpringCloudApp app, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile, app, project);
        this.project = project;
        this.app = app;
        this.draft = (SpringCloudAppDraft) this.app.update();
        this.rerender();
        this.initListeners();
    }

    @Override
    protected void rerender() {
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runLater(() -> {
            this.setEnabled(false);
            tm.runOnPooledThread(() -> {
                final SpringCloudDeployment deployment = this.draft.getActiveDeployment();
                if (deployment == null) {
                    AzureMessager.getMessager().warning(AzureString.format("No active deployment found for app(%s)", this.draft.getName()));
                    return;
                }
                this.deploymentDraft = (SpringCloudDeploymentDraft) (deployment.isDraft() ? deployment : deployment.update());
                this.draft.setActiveDeployment(this.deploymentDraft);
                tm.runLater(() -> {
                    this.formConfig.updateForm(this.draft);
                    this.panelInstances.setApp(this.draft);
                    this.lblSubscription.setText(this.draft.getSubscription().getName());
                    this.lblCluster.setText(this.draft.getParent().getName());
                    this.lblApp.setText(this.draft.getName());
                    this.refreshToolbar();
                    this.formConfig.setValue(this.draft);
                    this.setEnabled(true);
                });
            });
        });
    }

    private void initListeners() {
        this.resetButton.addActionListener(e -> this.reset());
        this.refreshButton.addActionListener(e -> refresh());
        final AzureString deleteTitle = OperationBundle.description("user/resource.delete_resource.resource", this.draft.getName());
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        this.deleteButton.addActionListener(e -> {
            final String message = String.format("Are you sure to delete Spring app(%s)", this.draft.getName());
            if (AzureMessager.getMessager().confirm(message, "Delete Spring app")) {
                tm.runInBackground(deleteTitle, () -> {
                    IntellijShowPropertiesViewAction.closePropertiesView(this.draft, this.project);
                    this.draft.delete();
                });
            }
        });
        final AzureString startTitle = OperationBundle.description("user/resource.start_resource.resource", this.draft.getName());
        this.startButton.addActionListener(e -> tm.runInBackground(startTitle, () -> {
            this.reset();
            this.app.start();
        }));
        final AzureString stopTitle = OperationBundle.description("user/resource.stop_resource.resource", this.draft.getName());
        this.stopButton.addActionListener(e -> tm.runInBackground(stopTitle, () -> {
            this.reset();
            this.app.stop();
        }));
        final AzureString restartTitle = OperationBundle.description("user/resource.restart_resource.resource", this.draft.getName());
        this.restartButton.addActionListener(e -> tm.runInBackground(restartTitle, () -> {
            this.reset();
            this.app.restart();
        }));
        final AzureString saveTitle = AzureString.format("Saving updates of app(%s)", this.draft.getName());
        this.saveButton.addActionListener(e -> tm.runInBackground(saveTitle, this::save));
        this.formConfig.setDataChangedListener(() -> {
            this.formConfig.applyTo(this.draft);
            this.refreshToolbar();
        });
    }

    private void save() {
        this.setEnabled(false);
        this.formConfig.applyTo(this.draft);
        AzureTaskManager.getInstance().runInBackground("Saving updates", () -> {
            final SpringCloudDeployment deployment = this.draft.getActiveDeployment();
            Optional.ofNullable(deployment).filter(AbstractAzResource::isDraft).ifPresent(d -> ((SpringCloudDeploymentDraft) d).commit());
            this.draft.commit();
        });
    }

    private void reset() {
        this.draft.reset();
        this.rerender();
    }

    @Override
    public boolean isModified() {
        return this.draft.isModified() || this.deploymentDraft.isModified();
    }

    protected void refresh() {
        this.draft.reset();
        this.draft.refresh();
        this.rerender();
    }

    private void setEnabled(boolean enabled) {
        this.resetButton.setVisible(enabled);
        this.saveButton.setEnabled(enabled);
        this.startButton.setEnabled(enabled);
        this.stopButton.setEnabled(enabled);
        this.restartButton.setEnabled(enabled);
        this.deleteButton.setEnabled(enabled);
        this.formConfig.setEnabled(enabled);
        this.panelInstances.setEnabled(enabled);
    }

    private void refreshToolbar() {
        // get status from app instead of draft since status of draft is not correct
        final AzResource.FormalStatus formalStatus = this.app.getFormalStatus();
        AzureTaskManager.getInstance().runLater(() -> {
            final boolean normal = formalStatus.isRunning() || formalStatus.isStopped();
            this.setEnabled(normal);
            if (normal) {
                final boolean modified = this.isModified();
                this.resetButton.setVisible(modified);
                this.saveButton.setEnabled(modified);
            } else {
                this.resetButton.setVisible(false);
                this.saveButton.setEnabled(false);
            }
            this.startButton.setEnabled(formalStatus.isStopped());
            this.stopButton.setEnabled(formalStatus.isRunning());
            this.restartButton.setEnabled(formalStatus.isRunning());
            this.deleteButton.setEnabled(!formalStatus.isWriting());
        });
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return contentPanel;
    }

    private void createUIComponents() {
    }
}
