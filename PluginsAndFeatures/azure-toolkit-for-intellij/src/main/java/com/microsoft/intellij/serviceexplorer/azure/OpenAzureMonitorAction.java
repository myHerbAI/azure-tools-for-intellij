/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.monitor.AzureMonitorManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class OpenAzureMonitorAction extends NodeAction {
    private static boolean isActionTriggered = false;
    private static final String AZURE_MONITOR_TRIGGERED = "AzureMonitor.Triggered";

    public OpenAzureMonitorAction(@Nonnull AzureModule azureModule) {
        super(azureModule, "Open Azure Monitor");
        addListener(new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent nodeActionEvent) {
                Optional.ofNullable(azureModule.getProject())
                        .ifPresent(project -> {
                            if (!isActionTriggered) {
                                isActionTriggered = true;
                                PropertiesComponent.getInstance().setValue(AZURE_MONITOR_TRIGGERED, true);
                            }
                            LogAnalyticsWorkspace defaultWorkspace = null;
                            final Account account = Azure.az(AzureAccount.class).account();
                            if (Objects.nonNull(account) && account.getSelectedSubscriptions().size() > 0) {
                                final Subscription subscription = account.getSelectedSubscriptions().get(0);
                                final List<LogAnalyticsWorkspace> workspaceList = Azure.az(AzureLogAnalyticsWorkspace.class)
                                        .logAnalyticsWorkspaces(subscription.getId()).list().stream().collect(Collectors.toList());
                                if (workspaceList.size() == 0) {
                                    AzureMessager.getMessager().info(message("azure.monitor.info.workspaceNotFoundInSub", subscription.getId()));
                                    return;
                                }
                                defaultWorkspace = workspaceList.get(0);
                            } else {
                                AzureMessager.getMessager().info(message("azure.monitor.info.selectSubscription"));
                                return;
                            }
                            AzureMonitorManager.getInstance().openMonitorWindow((Project) project, defaultWorkspace, null);
                        });
            }
        });
    }

    @Override
    public AzureIcon getIconSymbol() {
        if (!isActionTriggered) {
            isActionTriggered = PropertiesComponent.getInstance().getBoolean(AZURE_MONITOR_TRIGGERED);
        }
        return isActionTriggered ? AzureIcons.Common.AZURE_MONITOR : AzureIcons.Common.AZURE_MONITOR_NEW;
    }
}
