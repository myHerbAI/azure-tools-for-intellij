/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.IManagedIdentitySupported;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractConnectionStringAzResourceModule.CONNECTION_STRING_SUBSCRIPTION_ID;

@Getter
public abstract class BaseStorageAccountResourceDefinition extends AzureServiceResource.Definition<IStorageAccount>
    implements FunctionSupported<IStorageAccount>, IManagedIdentitySupported<IStorageAccount> {
    public static final int METHOD_AZURE = 0;
    public static final int METHOD_AZURITE = 1;
    public static final int METHOD_STRING = 2;

    public static final String LOCAL_STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    public static final String CONNECTION_STRING_KEY = String.format("%s_CONNECTION_STRING", Connection.ENV_PREFIX);
    public static final String ACCOUNT_NAME_KEY = String.format("%s_ACCOUNT_NAME", Connection.ENV_PREFIX);
    public static final String ACCOUNT_KEY = String.format("%s_ACCOUNT_KEY", Connection.ENV_PREFIX);

    public BaseStorageAccountResourceDefinition() {
        super("Azure.Storage", "Azure Storage Account", AzureIcons.StorageAccount.MODULE.getIconPath());
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<IStorageAccount> accountDef, Project project) {
        final HashMap<String, String> env = new HashMap<>();
        final IStorageAccount account = accountDef.getData();
        if (Objects.nonNull(account)) {
            final String conString = account.getConnectionString();
            env.put(CONNECTION_STRING_KEY, conString);
            env.put(ACCOUNT_NAME_KEY, account.getName());
            env.put(ACCOUNT_KEY, account.getKey());
        }
        return env;
    }

    @Override
    public IStorageAccount getResource(String dataId, final String id) {
        if (StringUtils.equalsIgnoreCase(dataId, AzuriteStorageAccount.AZURITE_RESOURCE_ID)) {
            return AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT;
        } else if (CONNECTION_STRING_SUBSCRIPTION_ID.equalsIgnoreCase(ResourceId.fromString(dataId).subscriptionId())) {
            final String connectionString = IntelliJSecureStore.getInstance().loadPassword(BaseStorageAccountResourceDefinition.class.getName(), id.toLowerCase(), null);
            if (StringUtils.isNotBlank(connectionString)) {
                return Azure.az(AzureStorageAccount.class).getOrInitByConnectionString(connectionString);
            }
            return null;
        } else {
            return Azure.az(AzureStorageAccount.class).getById(dataId);
        }
    }

    @Override
    public List<Resource<IStorageAccount>> getResources(Project project) {
        return Azure.az(AzureStorageAccount.class).list().stream()
            .flatMap(m -> m.storageAccounts().list().stream())
            .map(this::define).toList();
    }

    @Nonnull
    @Override
    public String getResourceType() {
        return "Storage";
    }

    @Override
    public List<String> getEnvironmentVariablesKey() {
        return ListUtils.union(Arrays.asList(CONNECTION_STRING_KEY, ACCOUNT_NAME_KEY, ACCOUNT_KEY), super.getEnvironmentVariablesKey());
    }

    @Nullable
    @Override
    public String getResourceConnectionString(@Nonnull IStorageAccount resource) {
        return resource instanceof AzuriteStorageAccount ? LOCAL_STORAGE_CONNECTION_STRING : resource.getConnectionString();
    }

    @Override
    public Map<String, String> initIdentityEnv(AzureServiceResource<IStorageAccount> accountDef, Project project) {
        final HashMap<String, String> env = new HashMap<>();
        final IStorageAccount account = accountDef.getData();
        if (Objects.nonNull(account)) {
            env.put(ACCOUNT_NAME_KEY, account.getName());
        }
        return env;
    }

    @Nullable
    @Override
    public Map<String, BuiltInRole> getBuiltInRoles() {
        return Map.of("ba92f5b4-2d11-453d-a403-e96b0029c9fe", BuiltInRole.STORAGE_BLOB_DATA_CONTRIBUTOR,
            "974c5e8b-45b9-4653-ba55-5f855dd0fb88", BuiltInRole.STORAGE_QUEUE_DATA_CONTRIBUTOR,
            "0c867c2a-1d8c-454a-a3db-ab2ea1bdc8bb", BuiltInRole.STORAGE_FILE_DATA_SMB_SHARE_CONTRIBUTOR);
    }
}
