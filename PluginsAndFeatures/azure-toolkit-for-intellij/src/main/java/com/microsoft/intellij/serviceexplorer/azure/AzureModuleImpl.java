/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure;

import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

public class AzureModuleImpl extends AzureModule {
    public AzureModuleImpl(@Nullable Object project) {
        super(project);
    }

    @Override
    protected void loadActions() {
        super.loadActions();
        addAction(new SignInOutAction(this), Groupable.DIAGNOSTIC_GROUP);
        addAction(new ManageSubscriptionsAction(this));
        addAction(new GetStartAction(this), Groupable.MAINTENANCE_GROUP);
        addAction(new OpenAzureMonitorAction(this), Groupable.MAINTENANCE_GROUP);
    }

    @Override
    protected void refreshFromAzure() throws AzureCmdException {
        super.refreshFromAzure();
        AzureExplorer.refreshAll();
    }
}
