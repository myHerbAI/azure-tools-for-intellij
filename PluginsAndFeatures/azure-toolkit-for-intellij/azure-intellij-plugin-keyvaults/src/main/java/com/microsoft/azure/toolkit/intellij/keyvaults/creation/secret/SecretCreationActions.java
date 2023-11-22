/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults.creation.secret;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.SecretDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SecretCreationActions {
    public static void createNewSecret(@Nonnull final KeyVault keyVault, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SecretCreationDialog dialog = new SecretCreationDialog("Create new secret");
            final Action.Id<SecretDraft.Config> actionId = Action.Id.of("user/keyvaults.create_secret.secret");
            dialog.setOkAction(new Action<>(actionId)
                    .withLabel("Create")
                    .withIdParam(SecretDraft.Config::getName)
                    .withAuthRequired(true)
                    .withHandler(config -> {
                        keyVault.createNewSecret(config);
                    }));
            dialog.show();
        });
    }

    public static void createNewSecretVersion(@Nonnull final Secret secret, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SecretCreationDialog dialog = new SecretCreationDialog(String.format("Create new version for secret %s", secret.getName()));
            final Action.Id<SecretDraft.Config> actionId = Action.Id.of("user/keyvaults.create_secret_version.secret|version");
            dialog.setOkAction(new Action<>(actionId)
                    .withLabel("Create")
                    .withIdParam(SecretDraft.Config::getName)
                    .withAuthRequired(true)
                    .withHandler(config -> {
                        secret.addNewSecretVersion(config);
                    }));
            dialog.setFixedName(secret.getName());
            dialog.show();
        });
    }
}
