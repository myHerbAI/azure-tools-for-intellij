/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;

public class AzResourceNode<T extends AzResource> extends Node<T> {
    private final AzureEventBus.EventListener listener;

    public AzResourceNode(@Nonnull T resource) {
        super(resource);
        this.withIcon(DEFAULT_AZURE_RESOURCE_ICON_PROVIDER::getIcon);
        this.withLabel(AzResource::getName);
        this.withDescription(AzResource::getStatus);
        this.enableWhen(r -> !r.getFormalStatus().isDeleted());

        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("resource.refreshed.resource", listener);
        AzureEventBus.on("resource.status_changed.resource", listener);
        AzureEventBus.on("resource.children_changed.resource", listener);
    }

    public void onEvent(AzureEvent event) {
        final T data = this.getValue();
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof AzResource &&
            StringUtils.equals(((AzResource) source).getId(), data.getId()) &&
            StringUtils.equals(((AzResource) source).getName(), data.getName())) {
            if (StringUtils.equals(type, "resource.refreshed.resource")) {
                this.refreshChildrenLater(false);
            } else if (StringUtils.equals(type, "resource.status_changed.resource")) {
                this.refreshViewLater();
            } else if (StringUtils.equals(type, "resource.children_changed.resource")) {
                this.refreshChildrenLater(true);
            }
        }
    }

    public void dispose() {
        super.dispose();
        AzureEventBus.off("resource.refreshed.resource", listener);
        AzureEventBus.off("resource.status_changed.resource", listener);
        AzureEventBus.off("resource.children_changed.resource", listener);
    }

    @Override
    public String buildDescription() {
        final boolean deleted = this.getValue().getFormalStatus().isDeleted();
        return deleted ? AzResource.Status.DELETED : super.buildDescription();
    }
}
