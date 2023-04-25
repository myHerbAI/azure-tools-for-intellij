/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsManager;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstanceSelectionDialog;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class SpringCloudStreamingLogAction {
    public static void startAppStreamingLogs(@Nonnull SpringCloudApp app, @Nullable Project project) {
        final IAzureMessager messager = AzureMessager.getMessager();
        final AzureString title = OperationBundle.description("internal/springcloud.open_log_stream.instance", app.getName());
        showLogStreamingDialog(app, project);
    }

    public static void startInstanceStreamingLogs(Project project, SpringCloudApp app, String instanceName) {
        final Flux<String> logs = Optional.ofNullable(app.getActiveDeployment()).map(d ->
                d.streamingLogs(app.getLogStreamingEndpoint(instanceName),
                        ImmutableMap.of("follow", String.valueOf(true),
                                "sinceSeconds", String.valueOf(300),
                                "tailLines", String.valueOf(500),
                                "limitBytes", String.valueOf(1024 * 1024)))).orElse(Flux.empty());
        AzureTaskManager.getInstance().runLater(() ->
                StreamingLogsManager.getInstance().showStreamingLog(project, app.getId(), app.getName(), logs));
    }

    private static void showLogStreamingDialog(SpringCloudApp app, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudAppInstanceSelectionDialog dialog = new SpringCloudAppInstanceSelectionDialog(project, app);
            if (dialog.showAndGet()) {
                final SpringCloudAppInstance target = dialog.getInstance();
                startInstanceStreamingLogs(project, app, target.getName());
            }
        });
    }
}
