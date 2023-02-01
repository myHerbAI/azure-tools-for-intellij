/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.updateimage;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;

import javax.annotation.Nonnull;
import java.util.Objects;

public class UpdateContainerImageAction {
    @AzureOperation(name = "user/containerapps.open_update_image_dialog.app", params = {"app.getName()"})
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

    @AzureOperation(name = "user/containerapps.update_image.app", params = {"app.getName()"})
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
