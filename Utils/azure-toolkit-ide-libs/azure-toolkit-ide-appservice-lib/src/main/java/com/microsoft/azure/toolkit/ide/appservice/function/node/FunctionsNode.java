/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.node;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionsNode extends Node<FunctionApp> {
    private final AzureEventBus.EventListener listener;

    public FunctionsNode(@Nonnull FunctionApp functionApp) {
        super(functionApp);
        this.withIcon(AzureIcons.FunctionApp.MODULE)
            .withLabel("Functions")
            .withActions(FunctionAppActionsContributor.FUNCTIONS_ACTIONS)
            .addChildren(a -> a.listFunctions().stream().sorted(Comparator.comparing(FunctionEntity::getName)).collect(Collectors.toList()), (a, functionsNode) -> new Node<>(a)
                .withIcon("/icons/function-trigger.png")
                .withLabel(FunctionEntity::getName)
                .withDescription(FunctionsNode::getFunctionTriggerType)
                .onDoubleClicked(FunctionAppActionsContributor.TRIGGER_FUNCTION)
                .withActions(FunctionAppActionsContributor.FUNCTION_ACTION));

        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("resource.refreshed.resource", listener);
        AzureEventBus.on("appservice|function.functions.refresh", listener);
    }

    private static String getFunctionTriggerType(@Nonnull FunctionEntity functionEntity) {
        return Optional.ofNullable(functionEntity.getTrigger()).map(FunctionEntity.BindingEntity::getType).map(StringUtils::capitalize).orElse("Unknown");
    }

    public void onEvent(AzureEvent event) {
        final Object source = event.getSource();
        if (source instanceof AzResource && ((AzResource) source).getId().equals(this.getValue().getId())) {
            this.refreshChildrenLater();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        AzureEventBus.off("resource.refreshed.resource", listener);
        AzureEventBus.off("appservice|function.functions.refresh", listener);
    }
}
