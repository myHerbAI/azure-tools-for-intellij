/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.deployment;

import com.microsoft.azure.toolkit.eclipse.common.artifact.AzureArtifactManager;
import com.microsoft.azure.toolkit.eclipse.common.console.AzureAsyncConsoleJob;
import com.microsoft.azure.toolkit.eclipse.common.console.EclipseConsoleMessager;
import com.microsoft.azure.toolkit.eclipse.common.console.JobConsole;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.jetbrains.annotations.Nullable;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class DeploySpringCloudAppAction {
    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String NOTIFICATION_TITLE = "Deploy Spring App";

    public static void deployToApp(@Nullable SpringCloudApp app) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudDeploymentDialog dialog = new SpringCloudDeploymentDialog(Display.getCurrent().getActiveShell());
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                if (Objects.nonNull(app)) {
                    SpringCloudAppConfig config = SpringCloudAppConfig.fromApp(app);
                    AzureTaskManager.getInstance().runLater(() -> dialog.getForm().setValue(config), AzureTask.Modality.ANY);
                }
            });
            dialog.setOkActionListener((config) -> {
                final boolean buildArtifact = dialog.getBuildArtifact();
                dialog.close();
                final IArtifact artifact = config.getDeployment().getArtifact();
                if (Objects.nonNull(artifact)) {
                    if (buildArtifact) {
                        AzureArtifactManager.buildArtifact(((WrappedAzureArtifact) artifact).getArtifact())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe((r) -> deployToApp(config), e -> AzureMessager.getMessager().error(e));
                    } else {
                        deployToApp(config);
                    }
                }
            });
            dialog.open();
        });
    }

    @AzureOperation(name = "user/springcloud.deploy", params = "config.getAppName()")
    private static void deployToApp(@Nonnull SpringCloudAppConfig config) {
        AzureTaskManager.getInstance().runLater(() -> {
            final AzureAsyncConsoleJob job = new AzureAsyncConsoleJob("Deploy to Azure Spring Apps");
            JobConsole myConsole = new JobConsole("Deploy to Azure Spring Apps", job);
            EclipseConsoleMessager messager = new EclipseConsoleMessager(myConsole);

            myConsole.activate();
            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new JobConsole[]{myConsole});
            ConsolePlugin.getDefault().getConsoleManager().showConsoleView(myConsole);

            job.setMessager(messager);
            job.setSupplier(() -> {
                final AzureString title = OperationBundle.description("springcloud.create_update_app", config.getAppName());
                final AzureTask<Void> task = new AzureTask<Void>(title, () -> execute(config, messager));
                task.setType("user");
                AzureTaskManager.getInstance().runImmediately(task).join();
                return Status.OK_STATUS;
            });
            job.schedule();
        });
    }

    @AzureOperation(name = "internal/springcloud.create_update_app.app", params = {"appConfig.getAppName()"})
    private static SpringCloudDeployment execute(SpringCloudAppConfig appConfig, IAzureMessager messager) {
        OperationContext.current().setMessager(messager);
        final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(appConfig);
        final SpringCloudDeployment deployment = task.execute();
        final SpringCloudApp app = deployment.getParent();
        final boolean hasArtifact = Optional.ofNullable(appConfig.getDeployment())
                .map(SpringCloudDeploymentConfig::getArtifact).map(IArtifact::getFile).isPresent();
        if (hasArtifact && !deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
            messager.warning(GET_DEPLOYMENT_STATUS_TIMEOUT, NOTIFICATION_TITLE);
        }
        printPublicUrl(app);
        return deployment;
    }

    private static void printPublicUrl(final SpringCloudApp app) {
        final IAzureMessager messager = AzureMessager.getMessager();
        if (!app.isPublicEndpointEnabled()) {
            return;
        }
        messager.info(String.format("Getting public url of app(%s)...", app.name()));
        String publicUrl = app.getApplicationUrl();
        if (StringUtils.isEmpty(publicUrl)) {
            publicUrl = Utils.pollUntil(() -> {
                app.refresh();
                return app.getApplicationUrl();
            }, StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            messager.warning("Failed to get application url", NOTIFICATION_TITLE);
        } else {
            messager.info(String.format("Application url: %s", publicUrl));
        }
    }
}
