/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.arm;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DeploymentNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Resource Groups";
    private static final String ICON = AzureIcons.Resources.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureResources.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull ViewType type) {
        return (type == ViewType.TYPE_CENTRIC && data instanceof AzureResources) || data instanceof ResourceDeployment;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureResources) {
            final Function<AzureResources, List<ResourceGroup>> groupsLoader = s -> s.list().stream()
                .flatMap(m -> m.resourceGroups().list().stream()).collect(Collectors.toList());
            return new AzServiceNode<>((AzureResources) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(ResourceGroupActionsContributor.TYPECENTRIC_RESOURCE_GROUPS_ACTIONS)
                .addChildren(groupsLoader, (d, p) -> manager.createNode(d, p, ViewType.TYPE_CENTRIC));
        } else if (data instanceof ResourceDeployment) {
            return new AzResourceNode<>((ResourceDeployment) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked(DeploymentActionsContributor.EDIT)
                .withActions(DeploymentActionsContributor.DEPLOYMENT_ACTIONS);
        }
        return null;
    }
}
