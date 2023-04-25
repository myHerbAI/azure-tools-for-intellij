/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.core.ui.SubscriptionsDialog;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;

public class SelectSubscriptionsCommandHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        onSelectSubscriptions(window.getShell());
        return null;
    }

    public static void onSelectSubscriptions(Shell parentShell) {
        try {
            if (!IdeAzureAccount.getInstance().isLoggedIn()) {
                return;
            }
			AzureTaskManager.getInstance().runLater(() -> SubscriptionsDialog.go(parentShell));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
