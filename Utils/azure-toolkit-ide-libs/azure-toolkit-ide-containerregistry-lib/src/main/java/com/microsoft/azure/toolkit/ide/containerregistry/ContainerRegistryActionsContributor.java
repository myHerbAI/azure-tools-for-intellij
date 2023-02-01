/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerregistry;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class ContainerRegistryActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.registry.service";
    public static final String REGISTRY_ACTIONS = "actions.registry.registry";

    public static final Action.Id<ContainerRegistry> PUSH_IMAGE = Action.Id.of("user/acr.push_image.acr");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_CONTAINER_REGISTRY = Action.Id.of("user/acr.create_registry.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(PUSH_IMAGE)
            .withLabel("Push Image")
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .withIdParam(AzResource::getName)
            .register(am);

        new Action<>(GROUP_CREATE_CONTAINER_REGISTRY)
            .withLabel("Container Registry")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler(s -> am.getAction(ResourceCommonActionsContributor.CREATE_IN_PORTAL).handle(Azure.az(AzureContainerRegistry.class)))
            .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE_IN_PORTAL
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup registryActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ContainerRegistryActionsContributor.PUSH_IMAGE
        );
        am.registerGroup(REGISTRY_ACTIONS, registryActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_CONTAINER_REGISTRY);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}

