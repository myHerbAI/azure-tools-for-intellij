/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.FullResourceGroupCreationDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourcesServiceSubscription;

public class IntellijResourceGroupActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.CREATE, s -> s instanceof AzureResources, this::createResourceGroup);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, s -> s instanceof ResourcesServiceSubscription, this::createResourceGroup);
    }

    private void createResourceGroup(Object o) {
        AzureTaskManager.getInstance().runLater(() -> {
            final Subscription s = o instanceof ResourcesServiceSubscription ? ((ResourcesServiceSubscription) o).getSubscription() : null;
            final FullResourceGroupCreationDialog dialog = new FullResourceGroupCreationDialog(s);
            dialog.setOkAction(new Action<ResourceGroupDraft>(Action.Id.of("user/arm.create_group.rg"))
                .withLabel("Create")
                .withIdParam(ResourceGroupDraft::getName)
                .withAuthRequired(true)
                .withHandler(AzResource.Draft::createIfNotExist));
            dialog.show();
        });
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1; //after azure resource common actions registered
    }
}
