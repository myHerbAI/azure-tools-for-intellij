/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class AzModuleNode<T extends AbstractAzResourceModule<?, ?, ?>> extends Node<T> {
    private final AzureEventBus.EventListener listener;

    public AzModuleNode(@Nonnull T module) {
        super(module);
        final String name = module.getResourceTypeName();
        this.withIcon(String.format("/icons/%s/default.svg", module.getFullResourceType()))
            .withLabel(name.endsWith("s") ? name : name + "s")
            .withMoreChildren(AbstractAzResourceModule::hasMoreResources, AbstractAzResourceModule::loadMoreResources);
        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("module.refreshed.module", listener);
        AzureEventBus.on("module.children_changed.module", listener);
    }

    public void dispose() {
        super.dispose();
        AzureEventBus.off("module.refreshed.module", listener);
        AzureEventBus.off("module.children_changed.module", listener);
    }

    public void onEvent(AzureEvent event) {
        final String type = event.getType();
        final Object source = event.getSource();
        final boolean childrenChanged = StringUtils.equalsIgnoreCase(type, "module.children_changed.module");
        if (source instanceof AzResourceModule && source.equals(this.getValue())) {
            this.refreshChildrenLater(childrenChanged);
        }
    }
}
