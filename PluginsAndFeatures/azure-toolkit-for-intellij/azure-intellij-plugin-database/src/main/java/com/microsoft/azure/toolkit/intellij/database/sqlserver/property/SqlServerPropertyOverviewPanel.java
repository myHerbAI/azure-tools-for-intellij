/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.property;

import com.microsoft.azure.toolkit.intellij.common.component.TextFieldUtils;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SqlServerPropertyOverviewPanel extends JPanel {
    private JPanel rootPanel;

    private JTextField resourceGroupTextField;

    private JTextField serverNameTextField;

    private JTextField statusTextField;

    private JTextField serverAdminLoginNameTextField;

    private JTextField locationTextField;

    private JTextField versionTextField;

    private JTextField subscriptionTextField;

    private JTextField subscriptionIDTextField;

    SqlServerPropertyOverviewPanel() {
        super();
        TextFieldUtils.disableTextBoard(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, versionTextField, subscriptionTextField, subscriptionIDTextField);
        TextFieldUtils.makeTextOpaque(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, versionTextField, subscriptionTextField, subscriptionIDTextField);
    }

    public void setFormData(MicrosoftSqlServer server) {
        final Subscription subscription = az(AzureAccount.class).account().getSubscription(server.getSubscriptionId());
        if (subscription != null) {
            subscriptionTextField.setText(subscription.getName());
        }
        resourceGroupTextField.setText(server.getResourceGroupName());
        statusTextField.setText(server.getStatus());
        locationTextField.setText(Optional.ofNullable(server.getRegion()).map(Region::getLabel).orElse(""));
        subscriptionIDTextField.setText(server.getSubscriptionId());
        serverNameTextField.setText(StringUtils.firstNonBlank(server.getFullyQualifiedDomainName(), server.getName()));
        serverNameTextField.setCaretPosition(0);
        serverAdminLoginNameTextField.setText(server.getFullAdminName());
        serverAdminLoginNameTextField.setCaretPosition(0);
        versionTextField.setText(server.getVersion());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

}
