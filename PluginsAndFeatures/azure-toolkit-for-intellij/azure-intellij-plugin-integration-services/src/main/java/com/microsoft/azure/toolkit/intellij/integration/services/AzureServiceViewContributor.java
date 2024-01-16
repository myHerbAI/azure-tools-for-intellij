/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.execution.services.ServiceViewLazyContributor;
import com.intellij.execution.services.SimpleServiceViewDescriptor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.intellij.explorer.TypeGroupedServicesRootNode;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AzureServiceViewContributor implements ServiceViewContributor<AzureResourceViewContributor>, ServiceViewLazyContributor {
    @Override
    public @NotNull ServiceViewDescriptor getViewDescriptor(@NotNull final Project project) {
        return new SimpleServiceViewDescriptor("Azure", IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
    }

    @Override
    public @NotNull List<AzureResourceViewContributor> getServices(@NotNull final Project project) {
        return Collections.singletonList(new AzureResourceViewContributor(new TypeGroupedServicesRootNode()
            .withActions(new ActionGroup(
                IAccountActions.SELECT_SUBS,
                "----",
                ResourceCommonActionsContributor.SHOW_COURSES,
                ResourceCommonActionsContributor.OPEN_MONITOR,
                "----",
                IAccountActions.AUTHENTICATE))
            .addChildren((a) -> buildAzServiceNodes())));
    }

    @Override
    public @NotNull ServiceViewDescriptor getServiceDescriptor(@NotNull final Project project, @NotNull final AzureResourceViewContributor contributor) {
        return contributor.getViewDescriptor(project);
    }

    @Nonnull
    public static List<Node<?>> buildAzServiceNodes() {
        final AzureExplorer.AzureExplorerNodeProviderManager manager = AzureExplorer.getManager();
        return manager.getAzServices().stream()
            .map(r -> manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
            .sorted(Comparator.comparing(Node::getLabel))
            .collect(Collectors.toList());
    }
}
