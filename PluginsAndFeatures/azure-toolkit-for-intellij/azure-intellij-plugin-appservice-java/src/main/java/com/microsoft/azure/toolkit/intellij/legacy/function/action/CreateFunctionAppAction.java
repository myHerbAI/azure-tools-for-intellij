/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppCreationDialog;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class CreateFunctionAppAction {
    public static void openDialog(final Project project, @Nullable final FunctionAppConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FunctionAppCreationDialog dialog = new FunctionAppCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.setData(data);
                dialog.toggleAdvancedMode(StringUtils.isNotBlank(data.environment()));
            }
            final Action.Id<FunctionAppConfig> actionId = Action.Id.of("user/function.create_app.app");
            dialog.setOkAction(new Action<>(actionId)
                                   .withLabel("Create")
                                   .withIdParam(FunctionAppConfig::appName)
                                   .withSource(config -> FunctionAppConfig.getResourceGroup(config))
                                   .withAuthRequired(true)
                                   .withHandler(config -> createFunctionApp(config, project)));
            dialog.show();
        });
    }

    private static void createFunctionApp(@Nonnull final FunctionAppConfig config, @Nullable Project project) {
        try {
            CacheManager.getUsageHistory(FunctionAppConfig.class).push(config);
            final CreateOrUpdateFunctionAppTask task = new CreateOrUpdateFunctionAppTask(config);
            final FunctionAppBase<?, ?, ?> execute = task.execute();
            if (execute instanceof AzResource.Draft) {
                ((AzResource.Draft<?, ?>) execute).reset();
            }
            OperationContext.current().setTelemetryProperties(getTelemetryProperties(config));
        } catch (final Exception error) {
            final Action<?> action = new Action<>(Action.Id.of("user/function.reopen_creation_dialog"))
                .withLabel("Reopen Create Function App dialog")
                .withHandler(t -> openDialog(project, config));
            AzureMessager.getMessager().error(error, null, action);
        }
    }

    public static Map<String, String> getTelemetryProperties(@Nullable final FunctionAppConfig config) {
        return Collections.emptyMap();
    }
}
