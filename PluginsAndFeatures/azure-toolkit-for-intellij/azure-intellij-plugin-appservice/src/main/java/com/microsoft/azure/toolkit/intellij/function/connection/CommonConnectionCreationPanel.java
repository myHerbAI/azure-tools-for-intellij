/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function.connection;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CommonConnectionCreationPanel implements AzureFormJPanel<Resource<ConnectionTarget>> {
    private JLabel lblConnectionName;
    private AzureTextInput txtConnectionName;
    private JLabel lblConnectionString;
    private AzureTextInput txtConnectionString;
    private JPanel pnlRoot;
    private String id = UUID.randomUUID().toString();

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(Resource<ConnectionTarget> data) {
        final ConnectionTarget target = data.getData();
        this.id = data.getDataId();
        this.txtConnectionName.setValue(target.getName());
        this.txtConnectionString.setValue(target.getConnectionString());
    }

    @Override
    public Resource<ConnectionTarget> getValue() {
        final String id = StringUtils.isEmpty(this.id) ? UUID.randomUUID().toString() : this.id;
        final String connectionName = txtConnectionName.getValue();
        final String connectionString = txtConnectionString.getValue();
        final ConnectionTarget target = ConnectionTarget.builder().id(id).connectionString(connectionString).name(connectionName).build();
        return CommonConnectionResource.Definition.INSTANCE.define(target);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtConnectionName, txtConnectionString);
    }
}
