/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

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
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
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

    public static void startInstanceStreamingLogs(Project project, @Nonnull SpringCloudAppInstance instance) {
        final SpringCloudCluster service = instance.getParent().getParent().getParent();
        final Flux<String> logs = Optional.of(instance)
            .map(d -> d.streamingLogs(true, service.isConsumptionTier() ? 300 : 500))
            .orElse(Flux.empty());
        final SpringCloudApp app = instance.getParent().getParent();
        AzureTaskManager.getInstance().runLater(() ->
            StreamingLogsManager.getInstance().showStreamingLog(project, app.getId(), app.getName(), logs));
    }

    private static void showLogStreamingDialog(SpringCloudApp app, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudAppInstanceSelectionDialog dialog = new SpringCloudAppInstanceSelectionDialog(project, app);
            if (dialog.showAndGet()) {
                final SpringCloudAppInstance target = dialog.getInstance();
                startInstanceStreamingLogs(project, target);
            }
        });
    }
}
