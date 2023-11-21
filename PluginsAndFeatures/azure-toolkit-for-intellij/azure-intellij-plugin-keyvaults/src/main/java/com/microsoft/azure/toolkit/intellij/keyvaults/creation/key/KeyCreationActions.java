/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults.creation.key;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvaults.key.Key;
import com.microsoft.azure.toolkit.lib.keyvaults.key.KeyDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KeyCreationActions {
    public static void createNewKey(@Nonnull final KeyVault keyVault, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final KeyCreationDialog dialog = new KeyCreationDialog("Create new key");
            final Action.Id<KeyDraft.Config> actionId = Action.Id.of("user/keyvaults.create_key.key");
            dialog.setOkAction(new Action<>(actionId)
                    .withLabel("Create")
                    .withIdParam(KeyDraft.Config::getName)
                    .withAuthRequired(true)
                    .withHandler(config -> {
                        keyVault.createNewKey(config);
                    }));
            dialog.show();
        });
    }

    public static void createNewKeyVersion(@Nonnull final Key key, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final KeyCreationDialog dialog = new KeyCreationDialog(String.format("Create new version for key %s", key.getName()));
            final Action.Id<KeyDraft.Config> actionId = Action.Id.of("user/keyvaults.create_key_version.key");
            dialog.setOkAction(new Action<>(actionId)
                    .withLabel("Create")
                    .withIdParam(KeyDraft.Config::getName)
                    .withAuthRequired(true)
                    .withHandler(config -> {
                        key.addNewKeyVersion(config);
                    }));
            dialog.setFixedName(key.getName());
            dialog.show();
        });
    }
}
