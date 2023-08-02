/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import rx.Single;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class CreateWebAppAction {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    @AzureOperation(name = "user/webapp.open_creation_dialog")
    public static void openDialog(final Project project, @Nullable final WebAppConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final WebAppCreationDialog dialog = new WebAppCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.setData(data);
            }
            dialog.setOkActionListener((config) -> {
                dialog.close();
                createWebApp(config)
                    .subscribe(webapp -> {
                        final Path artifact = config.getApplication();
                        if (Objects.nonNull(artifact) && artifact.toFile().exists()) {
                            AzureTaskManager.getInstance().runLater("deploy", () -> deploy(webapp, artifact, project));
                        }
                    }, (error) -> {
                        final Action<?> action = new Action<>(Action.Id.of("user/webapp.reopen_creation_dialog"))
                            .withLabel(String.format("Reopen dialog \"%s\"", dialog.getTitle()))
                            .withHandler(t -> openDialog(project, config));
                        AzureMessager.getMessager().error(error, null, action);
                    });
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "user/webapp.create_app.app", params = {"config.getName()"})
    private static Single<WebApp> createWebApp(final WebAppConfig config) {
        final AzureString title = description("user/webapp.create_app.app", config.getName());
        final AzureTask<WebApp> task = new AzureTask<>(null, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            return WebAppService.getInstance().createWebApp(config);
        });
        CacheManager.getUsageHistory(WebAppConfig.class).push(config);
        return AzureTaskManager.getInstance().runInBackgroundAsObservable(task).toSingle();
    }

    @AzureOperation(name = "user/webapp.deploy_artifact.app", params = {"webapp.name()"}, source = "webapp")
    private static void deploy(final WebApp webapp, final Path application, final Project project) {
        final AzureString title = description("user/webapp.deploy_artifact.app", webapp.getName());
        final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            final RunProcessHandler processHandler = new RunProcessHandler();
            processHandler.addDefaultListener();
            final ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
            processHandler.startNotify();
            consoleView.attachToProcess(processHandler);

            final RunProcessHandlerMessenger messenger = new RunProcessHandlerMessenger(processHandler);
            OperationContext.current().setMessager(messenger);
            AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(webapp, application.toFile(), true, processHandler);
        });
        AzureTaskManager.getInstance().runInBackgroundAsObservable(task).single().subscribe((none) -> notifyDeploymentSuccess(webapp)); // let root exception handler to show the error.
    }

    private static void notifyDeploymentSuccess(final WebApp app) {
        final String message = message("webapp.deploy.success.message", app.getName());
        AzureMessager.getMessager().success(message);
    }
}
