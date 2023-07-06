/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.ModuleComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.Getter;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ModulePanel implements AzureFormJPanel<Resource<String>> {
    @Getter
    private JPanel contentPanel;
    private ModuleComboBox moduleComboBox;

    private final Project project;

    public ModulePanel(Project project) {
        super();
        this.project = project;
    }

    @Override
    public Resource<String> getValue() {
        return ModuleResource.Definition.IJ_MODULE.define(moduleComboBox.getValue().getName());
    }

    @Override
    public void setValue(Resource<String> module) {
        Optional.ofNullable(module).ifPresent((m -> {
            final ItemReference<Module> val = new ItemReference<>(m.getData(), Module::getName);
            this.moduleComboBox.setValue(val, true);
        }));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.moduleComboBox);
    }

    private void createUIComponents() {
        moduleComboBox = new ModuleComboBox(project);
        moduleComboBox.setRequired(true);
    }
}
