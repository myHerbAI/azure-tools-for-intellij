/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp.handlers;

import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;

import java.util.Optional;

import com.microsoft.azure.toolkit.eclipse.common.logstream.EclipseAzureLogStreamingManager;

import reactor.core.publisher.Flux;

public class WebAppLogStreamingHandler {
    private static final String ENABLE_FILE_LOGGING = "Do you want to enable file logging for ({0})";
    private static final String ENABLE_LOGGING = "Enable logging";

    public static void stopLogStreaming(final WebAppBase<?, ?, ?> webApp) {
        AzureTaskManager.getInstance()
                .runLater(() -> EclipseAzureLogStreamingManager.getInstance().stopLogStreaming(webApp.id()));
    }

    public static void startLogStreaming(final WebAppBase<?, ?, ?> webApp) {
        if (!isLogStreamingEnabled(webApp)) {
            final boolean enableLogging = AzureMessager.getMessager()
                    .confirm(AzureString.format(ENABLE_FILE_LOGGING, webApp.name()), ENABLE_LOGGING);
            if (enableLogging) {
                enableLogStreaming(webApp);
            } else {
                return;
            }
        }
        final Flux<String> log = webApp.streamingLogs(true, null);
        if (log == null) {
            return;
        }
        AzureTaskManager.getInstance().runLater(
                () -> EclipseAzureLogStreamingManager.getInstance().showLogStreaming(webApp.id(), log));
    }

    private static boolean isLogStreamingEnabled(WebAppBase<?, ?, ?> webApp) {
        return Optional.ofNullable(webApp.getDiagnosticConfig()).map(DiagnosticConfig::isEnableWebServerLogging).orElse(false);
    }

    private static void enableLogStreaming(WebAppBase<?, ?, ?> webApp) {
        final DiagnosticConfig diagnosticConfig = Optional.ofNullable(webApp.getDiagnosticConfig()).orElseGet(DiagnosticConfig::new);
        diagnosticConfig.setEnableWebServerLogging(true);
        diagnosticConfig.setWebServerLogQuota(35);
        diagnosticConfig.setWebServerRetentionPeriod(0);
        if (webApp instanceof WebApp) {
            final WebAppDraft draft = (WebAppDraft) ((WebApp) webApp).update();
            draft.setDiagnosticConfig(diagnosticConfig);
            draft.updateIfExist();
        } else if (webApp instanceof WebAppDeploymentSlot) {
            final WebAppDeploymentSlotDraft draft = (WebAppDeploymentSlotDraft) ((WebAppDeploymentSlot) webApp).update();
            draft.setDiagnosticConfig(diagnosticConfig);
            draft.updateIfExist();
        }
    }
}
