/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.property;

import com.microsoft.azure.toolkit.intellij.common.component.TextFieldUtils;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class PostgrePropertyOverviewPanel extends JPanel {
    private JPanel rootPanel;

    private JTextField resourceGroupTextField;

    private JTextField serverNameTextField;

    private JTextField statusTextField;

    private JTextField serverAdminLoginNameTextField;

    private JTextField locationTextField;

    private JTextField postgreSqlVersionTextField;

    private JTextField subscriptionTextField;

    private JTextField performanceConfigurationsTextField;

    PostgrePropertyOverviewPanel() {
        super();
        TextFieldUtils.disableTextBoard(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, postgreSqlVersionTextField, subscriptionTextField, performanceConfigurationsTextField);
        TextFieldUtils.makeTextOpaque(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, postgreSqlVersionTextField, subscriptionTextField, performanceConfigurationsTextField);
    }

    public void setFormData(PostgreSqlServer server) {
        final Subscription subscription = az(AzureAccount.class).account().getSubscription(server.getSubscriptionId());
        if (subscription != null) {
            subscriptionTextField.setText(subscription.getName());
        }
        resourceGroupTextField.setText(server.getResourceGroupName());
        statusTextField.setText(server.getStatus());
        locationTextField.setText(Optional.ofNullable(server.getRegion()).map(Region::getLabel).orElse(""));
        serverNameTextField.setText(StringUtils.firstNonBlank(server.getFullyQualifiedDomainName(), server.getName()));
        serverNameTextField.setCaretPosition(0);
        serverAdminLoginNameTextField.setText(server.getFullAdminName());
        serverAdminLoginNameTextField.setCaretPosition(0);
        postgreSqlVersionTextField.setText(server.getVersion());
        final String skuTier = server.getSkuTier();
        final int storageGB = server.getStorageInMB() / 1024;
        final String performanceConfigurations = skuTier + ", " + storageGB + " GB";
        performanceConfigurationsTextField.setText(performanceConfigurations);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

}
