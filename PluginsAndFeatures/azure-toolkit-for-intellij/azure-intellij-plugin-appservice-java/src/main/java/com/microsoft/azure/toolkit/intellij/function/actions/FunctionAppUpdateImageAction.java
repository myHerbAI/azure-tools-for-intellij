/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.actions;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.DockerUtils;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppUpdateImageDialog;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class FunctionAppUpdateImageAction {
    public static void updateImage(@Nonnull FunctionApp target, @Nullable Project project) {
        final ContainerAppDraft.ImageConfig imageConfig = Optional.of(target)
                .map(FunctionApp::getDockerRuntimeConfig)
                .map(DockerUtils::convertRuntimeConfigToImageConfig).orElse(null);
        AzureTaskManager.getInstance().runLater(() -> {
            final FunctionAppUpdateImageDialog dialog = new FunctionAppUpdateImageDialog(project);
            dialog.setFunctionApp(target);
            Optional.ofNullable(imageConfig).ifPresent(dialog::setImage);
            dialog.setOkAction(new Action<FunctionAppConfig>(Action.Id.of("user/$appservice.update_image.app"))
                    .withLabel("Update Image")
                    .withSource(target)
                    .withIdParam(FunctionAppConfig::appName)
                    .withAuthRequired(true)
                    .withHandler(config -> new CreateOrUpdateFunctionAppTask(config).execute()));
            dialog.show();
        });
    }
}
