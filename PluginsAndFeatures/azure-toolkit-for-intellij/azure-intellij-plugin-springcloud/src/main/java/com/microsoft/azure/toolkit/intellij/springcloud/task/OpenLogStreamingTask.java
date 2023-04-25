/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.task;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import org.apache.commons.collections4.CollectionUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class OpenLogStreamingTask implements Task {
    public static final String SPRING_APP = "springApp";

    private final ComponentContext context;

    public OpenLogStreamingTask(@Nonnull final ComponentContext context) {
        this.context = context;
    }

    @Override
    public void execute() {
        final SpringCloudApp app = Objects.requireNonNull((SpringCloudApp) context.getParameter(SPRING_APP),
                "`springApp` is required to open log streaming");
        final List<SpringCloudAppInstance> instances = Optional.ofNullable(app.getActiveDeployment()).map(SpringCloudDeployment::getInstances).orElse(null);
        if (CollectionUtils.isEmpty(instances)) {
            AzureMessager.getMessager().warning(AzureString.format("App `%s` is still starting up, please try again later.", app.getName()));
        } else {
            final Flux<String> logs = Optional.ofNullable(app.getActiveDeployment()).map(d ->
                    d.streamingLogs(app.getLogStreamingEndpoint(instances.get(0).getName()),
                            ImmutableMap.of("follow", String.valueOf(true),
                                    "sinceSeconds", String.valueOf(300),
                                    "tailLines", String.valueOf(500),
                                    "limitBytes", String.valueOf(1024 * 1024)))).orElse(Flux.empty());
            AzureTaskManager.getInstance().runLater(() ->
                    StreamingLogsManager.getInstance().showStreamingLog(context.getProject(), app.getId(), app.getName(), logs));
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.springcloud.log_streaming";
    }
}
