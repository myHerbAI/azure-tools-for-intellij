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
            final AzureContainerRegistry service = ((AzureContainerRegistry) data);
            final Function<AzureContainerRegistry, List<ContainerRegistry>> registries = asc -> asc.list().stream().flatMap(m -> m.registry().list().stream())
                .collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(ContainerRegistryActionsContributor.SERVICE_ACTIONS)
                .addChildren(registries, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof ContainerRegistry) {
            final ContainerRegistry server = (ContainerRegistry) data;
            return new Node<>(server)
                .view(new AzureResourceLabelView<>(server, ContainerRegistry::getLoginServerUrl))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .doubleClickAction(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .actions(ContainerRegistryActionsContributor.REGISTRY_ACTIONS)
                .addChildren(r -> r.getRepositoryModule().list(), ((repository, registryNode) -> this.createNode(repository, registryNode, manager)))
                .hasMoreChildren(c -> c.getRepositoryModule().hasMoreResources())
                .loadMoreChildren(c -> c.getRepositoryModule().loadMoreResources());
        } else if (data instanceof Repository) {
            final Repository repository = (Repository) data;
            return new Node<>(repository)
                .view(new AzureResourceLabelView<>(repository, r -> ""))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .actions(ContainerRegistryActionsContributor.REPOSITORY_ACTIONS)
                .addChildren(r -> r.getArtifactModule().list().stream().flatMap(i -> i.getTagModule().list().stream()).collect(Collectors.toList()), ((tag, repositoryNode) -> this.createNode(tag, repositoryNode, manager)))
                .hasMoreChildren(c -> c.getArtifactModule().hasMoreResources())
                .loadMoreChildren(c -> c.getArtifactModule().loadMoreResources());
        } else if (data instanceof Tag) {
            final Tag tag = (Tag) data;
            return new Node<>(tag).view(new AzureResourceLabelView<>(tag, t -> t.getLastUpdatedOn().format(DateTimeFormatter.RFC_1123_DATE_TIME)))
                .actions(ContainerRegistryActionsContributor.TAG_ACTIONS);
        }
        return null;
    }
}
