package com.microsoft.azure.toolkit.intellij.keyvaults.creation.certificate;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.keyvaults.creation.secret.SecretCreationDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvaults.certificate.Certificate;
import com.microsoft.azure.toolkit.lib.keyvaults.certificate.CertificateDraft;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.SecretDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CertificateCreationActions {
    public static void createNewCertificate(@Nonnull final KeyVault keyVault, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CertificateCreationDialog dialog = new CertificateCreationDialog("Create new certificate");
            final Action.Id<CertificateDraft.Config> actionId = Action.Id.of("user/keyvaults.create_certificate.certificate|keyvault");
            dialog.setOkAction(new Action<>(actionId)
                    .withLabel("Create")
                    .withIdParam(CertificateDraft.Config::getName)
                    .withIdParam(keyVault.getName())
                    .withAuthRequired(true)
                    .withHandler(config -> {
                        keyVault.createNewCertificate(config);
                    }));
            dialog.show();
        });
    }

    public static void createNewCertificateVersion(@Nonnull final Certificate certificate, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CertificateCreationDialog dialog = new CertificateCreationDialog(String.format("Create new version for certificate %s", certificate.getName()));
            final Action.Id<CertificateDraft.Config> actionId = Action.Id.of("user/keyvaults.create_certificate_version.certificate");
            dialog.setOkAction(new Action<>(actionId)
                    .withLabel("Create")
                    .withIdParam(CertificateDraft.Config::getName)
                    .withAuthRequired(true)
                    .withHandler(config -> {
                        certificate.addNewCertificateVersion(config);
                    }));
            dialog.setFixedName(certificate.getName());
            dialog.show();
        });
    }
}
