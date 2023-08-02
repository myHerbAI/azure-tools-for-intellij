/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.updateimage;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;

import javax.annotation.Nonnull;
import java.util.Objects;

public class UpdateContainerImageAction {
    public static void openUpdateDialog(ContainerApp app, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(() -> {
            final UpdateImageDialog dialog = new UpdateImageDialog(e.getProject());
            if (Objects.nonNull(app)) {
                dialog.getForm().setApp(app);
            }
            dialog.setOkActionListener((config) -> {
                dialog.close();
                updateImage(config.getApp(), config.getImage());
            });
            dialog.show();
        });
    }

    public static void openUpdateDialog(Tag tag, AnActionEvent e) {
        final ContainerRegistry registry = tag.getParent().getParent().getParent();
        if (!registry.isAdminUserEnabled()) {
            final Action<ContainerRegistry> enableAdminUser = AzureActionManager.getInstance().getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(registry);
            throw new AzureToolkitRuntimeException(String.format("Admin user is not enabled for Azure Container Registry (%s).", registry.getName()), enableAdminUser);
        }
        AzureTaskManager.getInstance().runLater(() -> {
            final UpdateImageDialog dialog = new UpdateImageDialog(e.getProject());
            final UpdateImageForm.UpdateImageConfig config = new UpdateImageForm.UpdateImageConfig();
            final ContainerAppDraft.ImageConfig imageConfig = new ContainerAppDraft.ImageConfig(tag.getFullName());
            imageConfig.setContainerRegistry(registry);
            config.setImage(imageConfig);
            dialog.getForm().setValue(config);
            dialog.setOkActionListener((c) -> {
                dialog.close();
                updateImage(c.getApp(), c.getImage());
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "user/containerapps.update_image.app", params = {"app.getName()"}, source = "app")
    private static void updateImage(@Nonnull ContainerApp app, ContainerAppDraft.ImageConfig imageConfig) {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final ContainerAppDraft draft = (ContainerAppDraft) app.update();
            final ContainerAppDraft.Config config = new ContainerAppDraft.Config();
            config.setImageConfig(imageConfig);
            draft.setConfig(config);
            draft.updateIfExist();
        });
    }
}
