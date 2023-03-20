/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerregistry;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class ContainerRegistryNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Container Registries";
    private static final String ICON = AzureIcons.ContainerRegistry.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureContainerRegistry.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureContainerRegistry || data instanceof ContainerRegistry;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureContainerRegistry) {
            final AzureContainerRegistry service = ((AzureContainerRegistry) data);
            final Function<AzureContainerRegistry, List<ContainerRegistry>> registries = asc -> asc.list().stream().flatMap(m -> m.registry().list().stream())
                .collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(ContainerRegistryActionsContributor.SERVICE_ACTIONS)
                .addChildren(registries, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof ContainerRegistry) {
            final ContainerRegistry server = (ContainerRegistry) data;
            return new Node<>(server)
                .view(new AzureResourceLabelView<>(server))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .doubleClickAction(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .actions(ContainerRegistryActionsContributor.REGISTRY_ACTIONS);
        }
        return null;
    }
}
