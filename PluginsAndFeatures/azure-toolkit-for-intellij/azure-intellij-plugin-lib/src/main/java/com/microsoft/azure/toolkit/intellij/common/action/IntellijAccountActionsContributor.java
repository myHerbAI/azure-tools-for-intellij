/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;

import java.util.function.BiConsumer;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;

public class IntellijAccountActionsContributor implements IActionsContributor, IAccountActions {
    public static final String URL_TRY_AZURE_FOR_FREE = "https://azure.microsoft.com/en-us/free/?utm_campaign=javatools";

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureString tryAzureTitle = OperationBundle.description("account.try_aure");
        final ActionView.Builder tryAzureView = new ActionView.Builder("Try Azure for Free").title((s) -> tryAzureTitle);
        final BiConsumer<Object, AnActionEvent> tryAzureHandler = (Object v, AnActionEvent e) ->
            AzureActionManager.getInstance().getAction(OPEN_URL).handle(URL_TRY_AZURE_FOR_FREE);
        am.registerAction(new Action<>(IAccountActions.TRY_AZURE, tryAzureHandler, tryAzureView).setAuthRequired(false));
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1; //after azure resource common actions registered
    }
}
