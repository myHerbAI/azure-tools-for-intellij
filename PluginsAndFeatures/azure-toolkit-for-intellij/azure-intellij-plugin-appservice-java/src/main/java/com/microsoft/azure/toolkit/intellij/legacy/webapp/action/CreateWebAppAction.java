/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class CreateWebAppAction {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    public static void openDialog(final Project project, @Nullable final WebAppConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final WebAppCreationDialog dialog = new WebAppCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.setData(data);
            }
            final Action.Id<WebAppConfig> actionId = Action.Id.of("user/webapp.create_app.app");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(WebAppConfig::getName)
                .withSource(WebAppConfig::getResourceGroup)
                .withAuthRequired(true)
                .withHandler(config -> {
                    try {
                        CacheManager.getUsageHistory(WebAppConfig.class).push(config);
                        OperationContext.current().setTelemetryProperties(config.getTelemetryProperties());
                        final WebApp webapp = WebAppService.getInstance().createWebApp(config);
                        final Path artifact = config.getApplication();
                        if (Objects.nonNull(artifact) && artifact.toFile().exists()) {
                            new Action<>(Action.Id.<WebApp>of("user/webapp.deploy_artifact.app"))
                                .withIdParam(WebApp::getName)
                                .withSource(s -> s)
                                .withAuthRequired(true)
                                .withHandler(app -> deploy(app, artifact, project))
                                .handle(webapp);
                        }
                    } catch (final Exception error) {
                        final Action<?> action = new Action<>(Action.Id.of("user/webapp.reopen_creation_dialog"))
                            .withLabel(String.format("Reopen dialog \"%s\"", dialog.getTitle()))
                            .withHandler(t -> openDialog(project, config));
                        AzureMessager.getMessager().error(error, null, action);
                    }
                }));
            dialog.show();
        });
    }

    private static void deploy(final WebApp webapp, final Path application, final Project project) {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        final RunProcessHandler processHandler = new RunProcessHandler();
        processHandler.addDefaultListener();
        final ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);

        final RunProcessHandlerMessenger messenger = new RunProcessHandlerMessenger(processHandler);
        OperationContext.current().setMessager(messenger);
        AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(webapp, application.toFile(), true, processHandler);
        AzureMessager.getMessager().success(message("webapp.deploy.success.message", webapp.getName()));
    }
}
