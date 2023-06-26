/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice;

import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;

public class AppServiceDeploymentSlotsNode extends Node<AppServiceAppBase<?, ?, ?>> {
    private final AzureEventBus.EventListener listener;

    public AppServiceDeploymentSlotsNode(@Nonnull AppServiceAppBase<?, ?, ?> app) {
        super(app);
        this.withLabel("Deployment Slots");
        this.withIcon(AzureIcons.WebApp.DEPLOYMENT_SLOT);
        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("resource.refreshed.resource", listener);
        AzureEventBus.on("appservice.slot.refresh", listener);
    }

    @Override
    public void dispose() {
        super.dispose();
        AzureEventBus.off("resource.refreshed.resource", listener);
        AzureEventBus.off("appservice.slot.refresh", listener);
    }

    public void onEvent(AzureEvent event) {
        final Object source = event.getSource();
        if (source instanceof AzResource && ((AzResource) source).getId().equals(this.getValue().getId())) {
            this.refreshChildrenLater();
        }
    }
}
