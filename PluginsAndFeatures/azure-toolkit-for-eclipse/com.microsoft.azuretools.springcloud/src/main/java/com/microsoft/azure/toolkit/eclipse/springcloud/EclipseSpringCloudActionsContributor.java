/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud;

import com.microsoft.azure.toolkit.eclipse.springcloud.creation.CreateSpringCloudAppAction;
import com.microsoft.azure.toolkit.eclipse.springcloud.deployment.DeploySpringCloudAppAction;
import com.microsoft.azure.toolkit.eclipse.springcloud.streaminglog.SpringCloudLogStreamingHandler;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EclipseSpringCloudActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        this.registerCreateServiceActionHandler(am);
        this.registerCreateAppActionHandler(am);
        this.registerDeployAppActionHandler(am);
        this.registerLogStreamingActionHandler(am);
    }

    private void registerCreateServiceActionHandler(AzureActionManager am) {
        final Predicate<Object> condition = (r) -> r instanceof AzureSpringCloud;
        final Consumer<Object> handler = (r) -> {
            final IAccount account = Azure.az(IAzureAccount.class).account();
            final String url = String.format("%s/#create/Microsoft.AppPlatform", account.getPortalUrl());
            am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url, null);
        };
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);
    }

    private void registerCreateAppActionHandler(AzureActionManager am) {
        final Predicate<Object> condition = (r) -> r instanceof SpringCloudCluster;
        final Consumer<Object> handler = (c) -> CreateSpringCloudAppAction.createApp((SpringCloudCluster) c);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);
    }

    private void registerDeployAppActionHandler(AzureActionManager am) {
        final Predicate<AzResource> condition = (r) -> r instanceof SpringCloudApp;
        final Consumer<AzResource> handler = (c) -> DeploySpringCloudAppAction.deployToApp((SpringCloudApp) c);
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, condition, handler);
    }

    private void registerLogStreamingActionHandler(AzureActionManager am) {
        final Predicate<SpringCloudApp> condition = (r) -> r instanceof SpringCloudApp;
        final Consumer<SpringCloudApp> handler = (c) -> SpringCloudLogStreamingHandler.startLogStreaming(c);
        am.registerHandler(SpringCloudActionsContributor.STREAM_LOG_APP, condition, handler);
    }

    public int getOrder() {
        return SpringCloudActionsContributor.INITIALIZE_ORDER + 1;
    }
}
