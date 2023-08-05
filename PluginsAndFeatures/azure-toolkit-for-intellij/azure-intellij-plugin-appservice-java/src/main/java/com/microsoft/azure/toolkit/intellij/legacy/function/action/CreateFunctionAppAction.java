/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppCreationDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService;

import javax.annotation.Nullable;
import java.util.Objects;

public class CreateFunctionAppAction {
    public static void openDialog(final Project project, @Nullable final FunctionAppConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FunctionAppCreationDialog dialog = new FunctionAppCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.setData(data);
            }
            final Action.Id<FunctionAppConfig> actionId = Action.Id.of("user/function.create_app.app");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(FunctionAppConfig::getName)
                .withSource(FunctionAppConfig::getResourceGroup)
                .withAuthRequired(true)
                .withHandler(config -> {
                    try {
                        CacheManager.getUsageHistory(FunctionAppConfig.class).push(config);
                        OperationContext.current().setTelemetryProperties(config.getTelemetryProperties());
                        FunctionAppService.getInstance().createOrUpdateFunctionApp(config);
                    } catch (final Exception error) {
                        final Action<?> action = new Action<>(Action.Id.of("user/function.reopen_creation_dialog"))
                            .withLabel(String.format("Reopen dialog \"%s\"", dialog.getTitle()))
                            .withHandler(t -> openDialog(project, config));
                        AzureMessager.getMessager().error(error, null, action);
                    }
                }));
            dialog.show();
        });
    }
}
