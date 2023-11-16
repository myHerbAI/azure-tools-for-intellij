/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.keyvaults.KeyVaultActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvaults.certificate.Certificate;
import com.microsoft.azure.toolkit.lib.keyvaults.certificate.CertificateVersion;
import com.microsoft.azure.toolkit.lib.keyvaults.key.Key;
import com.microsoft.azure.toolkit.lib.keyvaults.key.KeyVersion;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.SecretVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class IntelliJKeyVaultsActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        final Function<CertificateVersion, byte[]> certificateContentFunction = CertificateVersion::getCertificatePem;
        final Function<CertificateVersion, String> certificateNameFunction = resource -> String.format("%s_%s.pem", resource.getParent().getName(), resource.getName());
        final BiPredicate<AbstractAzResource<?, ?, ?>, AnActionEvent> certificateCondition = (r, e) -> r instanceof Certificate;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL, certificateCondition,
                (c, e) -> downloadCredential(((Certificate) c).getCurrentVersion(), certificateContentFunction, certificateNameFunction, e.getProject()));

        final BiPredicate<AbstractAzResource<?, ?, ?>, AnActionEvent> certificateVersionCondition = (r, e) -> r instanceof CertificateVersion;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL, certificateCondition,
                (c, e) -> downloadCredential((CertificateVersion) c, certificateContentFunction, certificateNameFunction, e.getProject()));

        final Function<KeyVersion, byte[]> keyContentFunction = key -> key.getPublicKeyPem().getBytes();
        final Function<KeyVersion, String> keyNameFunction = resource -> String.format("%s_%s.pem", resource.getParent().getName(), resource.getName());
        final BiPredicate<AbstractAzResource<?, ?, ?>, AnActionEvent> keyCondition = (r, e) -> r instanceof Key;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL, keyCondition,
                (c, e) -> downloadCredential(((Key) c).getCurrentVersion(), keyContentFunction, keyNameFunction, e.getProject()));

        final BiPredicate<AbstractAzResource<?, ?, ?>, AnActionEvent> keyVersionCondition = (r, e) -> r instanceof KeyVersion;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL, keyVersionCondition,
                (c, e) -> downloadCredential((KeyVersion) c, keyContentFunction, keyNameFunction, e.getProject()));

        final Function<SecretVersion, byte[]> secretContentFunction = key -> key.getSecretValue().getBytes();
        final Function<SecretVersion, String> secretNameFunction = resource -> String.format("%s_%s.secret", resource.getParent().getName(), resource.getName());
        final BiPredicate<AbstractAzResource<?, ?, ?>, AnActionEvent> secretCondition = (r, e) -> r instanceof Secret;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL, secretCondition,
                (c, e) -> downloadCredential(((Secret) c).getCurrentVersion(), secretContentFunction, secretNameFunction, e.getProject()));

        final BiPredicate<AbstractAzResource<?, ?, ?>, AnActionEvent> secretVersionCondition = (r, e) -> r instanceof SecretVersion;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL, secretVersionCondition,
                (c, e) -> downloadCredential((SecretVersion) c, secretContentFunction, secretNameFunction, e.getProject()));
    }

    public static <T extends AbstractAzResource> void downloadCredential(@Nonnull final T resource,
                                                                         @Nonnull final Function<T, byte[]> contentFunction,
                                                                         @Nonnull final Function<T, String> nameFunction,
                                                                         @Nullable final Project project) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            fileChooserDescriptor.setTitle("Choose Where to Save the Credential");
            final VirtualFile vf = FileChooser.chooseFile(fileChooserDescriptor, null, null);
            if (vf != null) {
                final AzureString title = OperationBundle.description("boundary/keyvaults.download_credential.credential|dir", resource.getName(), vf.getPath());
                manager.runInModal(title, () -> {
                    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    indicator.setIndeterminate(true);
                    final String name = nameFunction.apply(resource);
                    final Path dest = Paths.get(Objects.requireNonNull(vf).getPath(), name);
                    final File destFile = dest.toFile();
                    try {
                        FileUtil.writeToFile(destFile, contentFunction.apply(resource));
                        if (destFile.exists()) {
                            VirtualFileActions.notifyDownloadSuccess(name, destFile, project);
                        }
                    } catch (IOException e) {
                        AzureMessager.getMessager().error(e);
                    }
                });
            }
        });
    }

    @Override
    public int getOrder() {
        return KeyVaultActionsContributor.INITIALIZE_ORDER + 1;
    }
}
