/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.streaminglog;

import org.eclipse.swt.widgets.Display;

import com.microsoft.azure.toolkit.eclipse.common.logstream.EclipseAzureLogStreamingManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;

import java.util.HashMap;
import java.util.Map;

public class SpringCloudLogStreamingHandler {
    public static void startLogStreaming(final SpringCloudApp app) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudStreamingLogDialog dialog = new SpringCloudStreamingLogDialog(
                    Display.getCurrent().getActiveShell(), app);
            dialog.setOkActionListener(instance -> {
                dialog.close();
                AzureTaskManager.getInstance().runLater(
                        () -> {
                            final Map<String, String> streamingLogsParamters = new HashMap<>();
                            streamingLogsParamters.put("follow", String.valueOf(true));
                            streamingLogsParamters.put("tailLines", String.valueOf(500));
                            streamingLogsParamters.put("sinceSeconds", String.valueOf(300));
                            streamingLogsParamters.put("limitBytes", String.valueOf(1024 * 1024));
                            EclipseAzureLogStreamingManager.getInstance().showLogStreaming(instance.name(),
                                    instance.name(), instance.streamingLogs(true));
                        });
            });
            dialog.open();
        });
    }
}
