/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.appservice.config.DeploymentSlotConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.intellij.util.ValidationUtils.isValidAppServiceName;

public class DeploymentSlotCreationDialog extends AzureDialog<DeploymentSlotConfig> implements AzureForm<DeploymentSlotConfig> {
    private static final String DO_NOT_CLONE_SETTINGS = "Do not clone settings";
    private static final String PARENT = "parent";

    private AzureTextInput txtName;
    private AzureComboBox<String> cbConfigurationSource;
    private JPanel pnlRoot;
    private final List<DeploymentSlotConfig> existingSlots;

    public DeploymentSlotCreationDialog(@Nullable Project project, @Nonnull List<DeploymentSlotConfig> existingSlots) {
        super(project);
        this.existingSlots = existingSlots;
        $$$setupUI$$$();
        this.cbConfigurationSource.setItemsLoader(() -> {
            final List<String> collect = existingSlots.stream().map(DeploymentSlotConfig::getName).collect(Collectors.toList());
            collect.add(0, PARENT);
            collect.add(DO_NOT_CLONE_SETTINGS);
            return collect;
        });
        this.txtName.addValidator(this::validateDeploymentSlotName);
        super.init();
    }

    @Override
    public AzureForm<DeploymentSlotConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Deployment Slot";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public DeploymentSlotConfig getValue() {
        final String source = StringUtils.equalsIgnoreCase(cbConfigurationSource.getValue(), DO_NOT_CLONE_SETTINGS) ?
                "new" : cbConfigurationSource.getValue();
        return DeploymentSlotConfig.builder()
                .name(txtName.getValue())
                .configurationSource(source)
                .build();
    }

    @Override
    public void setValue(DeploymentSlotConfig data) {
        txtName.setValue(data.getName());
        cbConfigurationSource.setValue(data.getConfigurationSource());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, cbConfigurationSource);
    }

    private AzureValidationInfo validateDeploymentSlotName() {
        final String value = txtName.getValue();
        if (StringUtils.isEmpty(value)) {
            return AzureValidationInfo.error(message("appService.name.validate.empty"), txtName);
        }
        if (value.length() < 2 || value.length() > 60) {
            return AzureValidationInfo.error(message("appService.name.validate.length"), txtName);
        }
        if (!isValidAppServiceName(value)) {
            return AzureValidationInfo.error(message("appService.name.validate.invalidName"), txtName);
        }
        final DeploymentSlotConfig existingSlot = existingSlots.stream()
                .filter(slot -> StringUtils.equalsIgnoreCase(slot.getName(), value))
                .findFirst().orElse(null);
        return existingSlot == null ? AzureValidationInfo.success(txtName) :
                AzureValidationInfo.error(String.format("Slot with name (%s) already exists", value), txtName);
    }

    void $$$setupUI$$$() {
    }
}
