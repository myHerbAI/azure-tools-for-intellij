/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerregistry;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Repository;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
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
        return data instanceof AzureContainerRegistry || data instanceof ContainerRegistry ||
            data instanceof Repository || data instanceof Tag;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureContainerRegistry) {
            final Function<AzureContainerRegistry, List<ContainerRegistry>> registries = asc -> asc.list().stream().flatMap(m -> m.registry().list().stream())
                .collect(Collectors.toList());
            return new AzServiceNode<>((AzureContainerRegistry) data)
                .withLabel(NAME)
                .withIcon(ICON)
                .withActions(ContainerRegistryActionsContributor.SERVICE_ACTIONS)
                .addChildren(registries, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof ContainerRegistry) {
            return new AzResourceNode<>((ContainerRegistry) data)
                .withDescription(ContainerRegistry::getLoginServerUrl)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .withActions(ContainerRegistryActionsContributor.REGISTRY_ACTIONS)
                .addChildren(r -> r.getRepositoryModule().list(), ((repository, registryNode) -> this.createNode(repository, registryNode, manager)))
                .withMoreChildren(c -> c.getRepositoryModule().hasMoreResources(), c -> c.getRepositoryModule().loadMoreResources());
        } else if (data instanceof Repository) {
            return new AzResourceNode<>((Repository) data)
                .withDescription(r -> "")
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(ContainerRegistryActionsContributor.REPOSITORY_ACTIONS)
                .addChildren(r -> r.getArtifactModule().list().stream().flatMap(i -> i.getTagModule().list().stream()).collect(Collectors.toList()), ((tag, repositoryNode) -> this.createNode(tag, repositoryNode, manager)))
                .withMoreChildren(c -> c.getArtifactModule().hasMoreResources(), c -> c.getArtifactModule().loadMoreResources());
        } else if (data instanceof Tag) {
            return new AzResourceNode<>((Tag) data)
                .withDescription(t -> t.getLastUpdatedOn().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .withActions(ContainerRegistryActionsContributor.TAG_ACTIONS);
        }
        return null;
    }
}
