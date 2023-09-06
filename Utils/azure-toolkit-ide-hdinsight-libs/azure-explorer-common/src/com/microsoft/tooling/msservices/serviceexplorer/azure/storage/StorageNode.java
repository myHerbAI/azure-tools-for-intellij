/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.HashMap;
import java.util.Map;

public class StorageNode extends Node implements TelemetryProperties {

    private final StorageAccount storageAccount;

    public StorageNode(Node parent, StorageAccount storageAccount) {
        super(storageAccount.id(), storageAccount.name(), parent, null, true);

        this.storageAccount = storageAccount;

        loadActions();
    }

    @Override
    public @Nullable AzureIcon getIconSymbol() {
        return AzureIcons.StorageAccount.MODULE;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.storageAccount.getSubscriptionId());
        properties.put(AppInsightsConstants.Region, this.storageAccount.getRegion().getName());
        return properties;
    }

    @AzureOperation(name = "user/storage.open_portal.account", params = {"this.storageAccount.name()"})
    private void openInPortal() {
        openResourcesInPortal(this.storageAccount.getSubscriptionId(), storageAccount.id());
    }

    @AzureOperation(name = "user/storage.delete_account.account", params = {"this.storageAccount.name()"})
    private void delete() {
        storageAccount.delete();
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        return super.initActions();
    }

    protected final BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(StorageModule.MODULE_NAME)
                .withInstanceName(name);
    }

}
