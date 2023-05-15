/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SpringCloudAppInstanceSelectionDialog extends DialogWrapper {

    private JPanel pnlRoot;
    private AzureComboBox<SpringCloudAppInstance> cbInstances;
    private JLabel tipsLabel;

    @Nullable
    private SpringCloudAppInstance instance;
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app %s.";
    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app.";
    private static final String FAILED_TO_LIST_INSTANCES = "Failed to list Spring app instances.";
    private static final String FAILED_TO_LIST_INSTANCES_WITH_MESSAGE = "Failed to list Spring app instances: %s";

    public SpringCloudAppInstanceSelectionDialog(@Nullable final Project project, SpringCloudApp app) {
        super(project, false);
        setTitle("Select Instance");
        init();
        this.tipsLabel.setIcon(AllIcons.General.ContextHelp);
        cbInstances.setRenderer(new ListCellRendererWithRightAlignedComponent<>() {
            @Override
            protected void customize(final SpringCloudAppInstance deploymentInstance) {
                setLeftText(deploymentInstance.getName());
            }
        });
        cbInstances.setItemsLoader(() -> {
            final SpringCloudDeployment deployment = app.getActiveDeployment();
            if (deployment == null || !deployment.exists()) {
                AzureMessager.getMessager().warning(NO_ACTIVE_DEPLOYMENT);
                tipsLabel.setVisible(true);
                return Collections.emptyList();
            }
            final List<SpringCloudAppInstance> instances = deployment.getInstances();
            if (CollectionUtils.isEmpty(instances)) {
                AzureMessager.getMessager().error(String.format(NO_AVAILABLE_INSTANCES, app.getName()));
                tipsLabel.setVisible(true);
                return Collections.emptyList();
            }
            tipsLabel.setVisible(false);
            return instances;
        });
    }

    @Nonnull
    public SpringCloudAppInstance getInstance() {
        return Objects.requireNonNull(instance, "Instance is required.");
    }

    @Override
    protected void doOKAction() {
        instance = (SpringCloudAppInstance) cbInstances.getSelectedItem();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        instance = null;
        super.doCancelAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }

    private void createUIComponents() {
        this.cbInstances = new AzureComboBox<>() {
            @Override
            protected String getItemText(Object item) {
                if (item == null) {
                    return StringUtils.EMPTY;
                }
                return ((SpringCloudAppInstance) item).getName();
            }
        };
    }
}
