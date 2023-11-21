/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.keyvaults.KeyVaultActionsContributor;
import com.microsoft.azure.toolkit.intellij.keyvaults.creation.certificate.CertificateCreationActions;
import com.microsoft.azure.toolkit.intellij.keyvaults.creation.key.KeyCreationActions;
import com.microsoft.azure.toolkit.intellij.keyvaults.creation.secret.SecretCreationActions;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.keyvaults.CredentialVersion;
import com.microsoft.azure.toolkit.lib.keyvaults.certificate.Certificate;
import com.microsoft.azure.toolkit.lib.keyvaults.certificate.CertificateModule;
import com.microsoft.azure.toolkit.lib.keyvaults.key.Key;
import com.microsoft.azure.toolkit.lib.keyvaults.key.KeyModule;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.SecretModule;

import java.util.function.BiPredicate;

public class IntelliJKeyVaultsActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<CredentialVersion, AnActionEvent> certificateCondition = (r, e) -> r instanceof CredentialVersion;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL_VERSION, certificateCondition,
                (CredentialVersion r, AnActionEvent e) -> KeyVaultCredentialActions.downloadCredential(r, e.getProject()));

        am.registerHandler(KeyVaultActionsContributor.SHOW_CREDENTIAL_VERSION, certificateCondition,
                (CredentialVersion r, AnActionEvent e) -> KeyVaultCredentialActions.showCredential(r, e.getProject()));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof SecretModule,
                (Object r, AnActionEvent e) -> SecretCreationActions.createNewSecret(((SecretModule) r).getParent(), e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof Secret,
                (Object r, AnActionEvent e) -> SecretCreationActions.createNewSecretVersion((Secret) r, e.getProject()));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof CertificateModule,
                (Object r, AnActionEvent e) -> CertificateCreationActions.createNewCertificate(((CertificateModule) r).getParent(), e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof Certificate,
                (Object r, AnActionEvent e) -> CertificateCreationActions.createNewCertificateVersion((Certificate) r, e.getProject()));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof KeyModule,
                (Object r, AnActionEvent e) -> KeyCreationActions.createNewKey(((KeyModule) r).getParent(), e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof Key,
                (Object r, AnActionEvent e) -> KeyCreationActions.createNewKeyVersion((Key) r, e.getProject()));
    }

    @Override
    public int getOrder() {
        return KeyVaultActionsContributor.INITIALIZE_ORDER + 1;
    }
}
