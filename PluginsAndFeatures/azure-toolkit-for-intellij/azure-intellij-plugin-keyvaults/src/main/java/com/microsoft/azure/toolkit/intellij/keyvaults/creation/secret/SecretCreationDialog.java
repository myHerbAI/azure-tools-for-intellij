/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults.creation.secret;

import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.SecretDraft;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class SecretCreationDialog extends AzureDialog<SecretDraft.Config> implements AzureFormPanel<SecretDraft.Config> {
    private final String title;
    private JPanel pnlRoot;
    private JLabel lblName;
    private AzureTextInput txtName;
    private JLabel lblValue;
    private AzureTextInput txtValue;

    public SecretCreationDialog(String title) {
        super();
        this.title = title;
        setTitle(this.title);
        $$$setupUI$$$();
        init();
    }

    @Override
    public AzureForm<SecretDraft.Config> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return this.title;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public SecretDraft.Config getValue() {
        final SecretDraft.Config config = new SecretDraft.Config();
        config.setName(txtName.getValue());
        config.setValue(txtValue.getValue());
        return config;
    }

    @Override
    public void setValue(@Nonnull final SecretDraft.Config data) {
        txtName.setValue(data.getName());
        txtValue.setValue(data.getValue());
    }

    public void setFixedName(final String name) {
        txtName.setValue(name);
        txtName.setEnabled(false);
        txtName.setEditable(false);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, txtValue);
    }
}
