/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class AzServiceNode<T extends AbstractAzService<?, ?>> extends Node<T> {
    private final AzureEventBus.EventListener listener;

    public AzServiceNode(@Nonnull T service) {
        super(service);
        this.withIcon(String.format("/icons/%s/default.svg", service.getFullResourceType()))
            .withLabel(service.getResourceTypeName())
            .withMoreChildren(AbstractAzResourceModule::hasMoreResources, AbstractAzResourceModule::loadMoreResources);

        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("module.refreshed.module", listener);
        AzureEventBus.on("module.children_changed.module", listener);
        AzureEventBus.on("service.children_changed.service", listener);
    }

    public void dispose() {
        super.dispose();
        AzureEventBus.off("module.refreshed.module", listener);
        AzureEventBus.off("module.children_changed.module", listener);
        AzureEventBus.off("service.children_changed.service", listener);
    }

    protected void onEvent(AzureEvent event) {
        final String type = event.getType();
        final Object source = event.getSource();
        final boolean childrenChanged = StringUtils.equalsAnyIgnoreCase(type, "module.children_changed.module", "service.children_changed.service");
        if (source instanceof AzService && source.equals(this.getValue())) {
            this.refreshChildrenLater(childrenChanged);
        }
    }
}
