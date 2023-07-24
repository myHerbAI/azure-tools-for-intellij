/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
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

public class SpringCloudAppInstanceSelectionDialog extends AzureDialog<SpringCloudAppInstance> implements AzureForm<SpringCloudAppInstance> {

    private final SpringCloudApp app;
    private JPanel pnlRoot;
    private AzureComboBox<SpringCloudAppInstance> cbInstances;
    private JLabel tipsLabel;

    @Nullable
    private SpringCloudAppInstance instance;
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app %s.";
    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app.";
    private static final String FAILED_TO_LIST_INSTANCES = "Failed to list Spring app instances.";
    private static final String FAILED_TO_LIST_INSTANCES_WITH_MESSAGE = "Failed to list Spring app instances: %s";

    public SpringCloudAppInstanceSelectionDialog(@Nullable final Project project, @Nonnull SpringCloudApp app) {
        super(project);
        this.app = app;
        init();
        this.tipsLabel.setIcon(AllIcons.General.ContextHelp);
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
        this.cbInstances.setRequired(true);
        this.cbInstances.setRenderer(new ListCellRendererWithRightAlignedComponent<>() {
            @Override
            protected void customize(final SpringCloudAppInstance deploymentInstance) {
                setLeftText(deploymentInstance.getName());
            }
        });
        this.cbInstances.setItemsLoader(() -> {
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

    @Override
    public AzureForm<SpringCloudAppInstance> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Select Instance";
    }

    @Override
    public SpringCloudAppInstance getValue() {
        return (SpringCloudAppInstance) cbInstances.getSelectedItem();
    }

    @Override
    public void setValue(SpringCloudAppInstance data) {
        cbInstances.setValue(data);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.cbInstances);
    }
}
