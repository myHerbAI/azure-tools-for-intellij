/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.action;

import java.util.Objects;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class EclipseActionsContributor implements IActionsContributor {

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    }

    @Override
    public void registerActions(AzureActionManager am) {
    	new Action<>(Action.REQUIRE_AUTH)
    	.withHandler((c, e) -> {
            AzureTaskManager.getInstance().runLater(() -> {
                SignInCommandHandler.requireSignedIn(PluginUtil.getParentShell(), c);
            });
        })
    	.withAuthRequired(false)
    	.register(am);
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.<String>registerHandler(ResourceCommonActionsContributor.OPEN_URL, Objects::nonNull, PluginUtil::openLinkInBrowser);
    }

}
