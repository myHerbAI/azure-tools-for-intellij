/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccountDraft;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CreateStorageAccountAction {
    public static void create(@Nonnull Project project, @Nullable final StorageAccountConfig data) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final StorageAccountCreationDialog dialog = new StorageAccountCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            dialog.setOkActionListener((config) -> {
                dialog.close();
                create(config);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "storage.create_account.account", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    public static void create(final StorageAccountConfig config) {
        final AzureString title = OperationBundle.description("storage.create_account.account", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> createStorageAccount(config));
    }

    public static StorageAccount createStorageAccount(StorageAccountConfig config) {
        final String subscriptionId = config.getSubscription().getId();
        OperationContext.action().setTelemetryProperty("subscriptionId", subscriptionId);
        if (config.getResourceGroup().isDraftForCreating()) { // create resource group if necessary.
            final ResourceGroup newResourceGroup = Azure.az(AzureResources.class)
                .groups(subscriptionId).createResourceGroupIfNotExist(config.getResourceGroup().getName(), config.getRegion());
            config.setResourceGroup(newResourceGroup);
        }
        final AzureStorageAccount az = Azure.az(AzureStorageAccount.class);
        final StorageAccountDraft draft = az.accounts(config.getSubscriptionId()).create(config.getName(), config.getResourceGroupName());
        draft.setConfig(config);
        final StorageAccount resource = draft.commit();
        CacheManager.getUsageHistory(StorageAccount.class).push(draft);
        return resource;
    }
}
