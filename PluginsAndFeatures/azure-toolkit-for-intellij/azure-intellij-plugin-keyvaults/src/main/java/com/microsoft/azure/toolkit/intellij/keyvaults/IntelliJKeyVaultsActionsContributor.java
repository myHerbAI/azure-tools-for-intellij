/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.keyvaults.KeyVaultActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.keyvaults.CredentialVersion;

import java.util.function.BiPredicate;

public class IntelliJKeyVaultsActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<CredentialVersion, AnActionEvent> certificateCondition = (r, e) -> r instanceof CredentialVersion;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL_VERSION, certificateCondition,
                (CredentialVersion r, AnActionEvent e) -> KeyVaultCredentialActions.downloadCredential(r, e.getProject()));

        am.registerHandler(KeyVaultActionsContributor.SHOW_CREDENTIAL_VERSION, certificateCondition,
                (CredentialVersion r, AnActionEvent e) -> KeyVaultCredentialActions.showCredential(r, e.getProject()));

    }

    @Override
    public int getOrder() {
        return KeyVaultActionsContributor.INITIALIZE_ORDER + 1;
    }
}
