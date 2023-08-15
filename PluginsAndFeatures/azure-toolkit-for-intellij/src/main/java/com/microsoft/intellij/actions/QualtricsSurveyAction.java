/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbAware;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.jetbrains.annotations.NotNull;

public class QualtricsSurveyAction extends AnAction implements DumbAware {

    private static final String SURVEY_URL = "https://microsoft.qualtrics.com/jfe/form/SV_b17fG5QQlMhs2up?" +
            "toolkit=%s&ide=%s&os=%s&jdk=%s&id=%s";

    public QualtricsSurveyAction() {
        super("Provide Feedback");
    }

    @Override
    @AzureOperation(name = "user/common.open_qualtrics_survey")
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(getRequestUrl());
    }

    private String getRequestUrl() {
        final IdeaPluginDescriptor pluginDescriptor = PluginManager
                .getPlugin(PluginId.getId("com.microsoft.tooling.msservices.intellij.azure"));
        final ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        final String toolkit = pluginDescriptor.getVersion();
        final String ide = String.format("%s %s", applicationInfo.getFullVersion(), applicationInfo.getBuild());
        final String os = System.getProperty("os.name");
        final String jdk = String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version"));
        final String id = AppInsightsClient.getInstallationId();
        return String.format(SURVEY_URL, toolkit, ide, os, jdk, id);
    }
}
