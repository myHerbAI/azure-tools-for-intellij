/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.cloneDialog.VcsCloneDialog;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.samples.view.AzureSamplesCloneDialogExtension;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzComponent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class IntelliJAzureSamplesActionsContributor implements IActionsContributor {
    private static final Map<String, String> SERVICE_KEYWORDS = new HashMap<>() {
        {
            put("Microsoft.App", "Container App");
            put("Microsoft.App/containerApps", "Container App");
            put("Microsoft.ContainerService", "Kubernetes");
            put("Microsoft.ContainerService/managedClusters", "Kubernetes");
            put("Microsoft.Resources", "Resource Group");
            put("Microsoft.Resources/resourceGroups", "Resource Group");
            put("Microsoft.Web", "WebApp");
            put("Microsoft.Web/sites", "WebApp");
            put("Microsoft.Compute", "Virtual Machine");
            put("Microsoft.Compute/virtualMachines", "Virtual Machine");
            put("Microsoft.ContainerRegistry", "Container Registry");
            put("Microsoft.ContainerRegistry/registries", "Container Registry");
            put("Microsoft.DBforMySQL", "MySQL");
            put("Microsoft.DBforMySQL/servers", "MySQL");
            put("Microsoft.DBforMySQL/flexibleServers", "MySQL");
            put("Microsoft.DBforPostgreSQL", "PostgreSQL");
            put("Microsoft.DBforPostgreSQL/servers", "PostgreSQL");
            put("Microsoft.DBforPostgreSQL/flexibleServers", "PostgreSQL");
            put("Microsoft.Cache", "Redis");
            put("Microsoft.Cache/Redis", "Redis");
            put("Microsoft.AppPlatform", "Spring");
            put("Microsoft.AppPlatform/Spring", "Spring");
            put("Microsoft.Sql", "SQL");
            put("Microsoft.Sql/servers", "SQL");
            put("Microsoft.Storage", "Azure Storage");
            put("Microsoft.Storage/storageAccounts", "Azure Storage");
            put("Microsoft.Insights", "Application Insights");
            put("Microsoft.Insights/components", "Application Insights");
            put("Microsoft.DocumentDB", "Cosmos DB");
            put("Microsoft.DocumentDB/databaseAccounts", "Cosmos DB");
            put("Microsoft.HDInsight", "HDInsight");
            put("Microsoft.HDInsight/clusters", "HDInsight");
            put("Microsoft.EventHub", "Event Hubs");
            put("Microsoft.EventHub/namespaces", "Event Hubs");
            put("Microsoft.ServiceBus", "Service Bus");
            put("Microsoft.ServiceBus/namespaces", "Service Bus");
            put("Microsoft.CognitiveServices", "OpenAI");
            put("Microsoft.CognitiveServices/accounts", "OpenAI");
            put("Microsoft.KeyVault", "Key Vault");
            put("Microsoft.KeyVault/vaults", "Key Vault");
            put("Microsoft.KeyVault/vaults/keys", "Key Vault, Keys");
            put("Microsoft.KeyVault/vaults/secrets", "Key Vault, Secrets");
            put("Microsoft.KeyVault/vaults/certificates", "Key Vault, Certificates");
        }
    };

    @Override
    public void registerActions(final AzureActionManager am) {
        new Action<>(ResourceCommonActionsContributor.BROWSE_AZURE_SAMPLES)
            .withLabel("Browse Sample Projects...")
            .withHandler((c, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final AnActionEvent event = (AnActionEvent) e;
                final VcsCloneDialog cloneDialog = new VcsCloneDialog.Builder(Objects.requireNonNull(event.getProject()))
                    .forExtension(AzureSamplesCloneDialogExtension.class);
                // refer to GetFromVersionControlAction.actionPerformed
                if (cloneDialog.showAndGet()) {
                    final CheckoutProvider.Listener listener = ProjectLevelVcsManager.getInstance(event.getProject()).getCompositeCheckoutListener();
                    cloneDialog.doClone(listener);
                }
            }))
            .withAuthRequired(false)
            .register(am);

        new Action<>(ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES)
            .withLabel("Browse Sample Projects...")
            .withIdParam(AzComponent::getResourceTypeName)
            .withHandler((c, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final AnActionEvent event = (AnActionEvent) e;
                final VcsCloneDialog dialog = new VcsCloneDialog.Builder(Objects.requireNonNull(event.getProject()))
                    .forExtension(AzureSamplesCloneDialogExtension.class);
                final JComponent component = dialog.getPreferredFocusedComponent();
                if (Objects.nonNull(component)) {
                    final SearchTextField search = (SearchTextField) component;
                    if (StringUtils.containsIgnoreCase(c.getClass().getName(), "function")) {
                        search.setText("Functions");
                    } else {
                        search.setText(SERVICE_KEYWORDS.get(c.getFullResourceType()));
                    }
                }
                dialog.show();
            }))
            .withAuthRequired(false)
            .register(am);

    }
}
