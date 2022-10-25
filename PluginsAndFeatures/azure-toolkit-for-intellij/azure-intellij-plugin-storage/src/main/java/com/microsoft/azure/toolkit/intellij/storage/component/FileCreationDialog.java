/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.component;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class FileCreationDialog extends AzureDialog<String>
    implements AzureForm<String> {
    private JBLabel labelDescription;
    private JPanel contentPanel;
    private FileNameTextField textName;

    public FileCreationDialog(StorageFile parent, Project project, String dialogTitle) {
        super(project);
        this.setTitle(dialogTitle);
        this.init();
        this.textName.setParent(parent);
        this.pack();
    }

    protected String getDialogTitle() {
        return "New File/Directory";
    }

    @Override
    public AzureForm<String> getForm() {
        return this;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public String getValue() {
        return this.textName.getValue();
    }

    @Override
    public void setValue(final String name) {
        this.textName.setValue(name);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }
}
