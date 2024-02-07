/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.execution.services.ServiceViewLazyContributor;
import com.intellij.execution.services.SimpleServiceViewDescriptor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.ActionNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.intellij.explorer.TypeGroupedServicesRootNode;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureServiceViewContributor implements ServiceViewContributor<NodeViewContributor>, ServiceViewLazyContributor {
    public AzureServiceViewContributor() {
        AzureEventBus.on("account.logged_in.account", new AzureEventBus.EventListener((e) -> AzureResourceManagerListener.resourceChanged()));
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener((e) -> AzureResourceManagerListener.resourceChanged()));
    }

    @Override
    public @Nonnull ServiceViewDescriptor getViewDescriptor(@Nonnull final Project project) {
        return new SimpleServiceViewDescriptor("Azure", IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)); // hidden root
    }

    @Override
    public @Nonnull List<NodeViewContributor> getServices(@Nullable final Project project) {
        final NodeViewContributor o = new NodeViewContributor(new RootNode() // visible root
            .withChildrenLoadLazily(true)
            .withActions(new ActionGroup(
                IAccountActions.SELECT_SUBS,
                IAccountActions.AUTHENTICATE,
                "----",
                AzureResourceActionsContributor.ADD_RESOURCE
            ))
            .addChildren((a) -> buildChildrenNodes()));
        return Collections.singletonList(o);
    }

    @Override
    public @Nonnull ServiceViewDescriptor getServiceDescriptor(@Nonnull final Project project, @Nonnull final NodeViewContributor contributor) {
        return contributor.getViewDescriptor(project);
    }

    @Nonnull
    public static List<Node<?>> buildChildrenNodes() {
        final AzureResourceManager resourceManager = AzureResourceManager.getInstance();
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            final Action<Object> action = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE).withLabel("Sign in to manage Azure resources...");
            return Collections.singletonList(new ActionNode<>(action));
        } else if (resourceManager.getResources().size() < 1) {
            return Collections.singletonList(new ActionNode<>(AzureResourceActionsContributor.ADD_RESOURCE));
        } else {
            return buildResourceNodes();
        }
    }

    @Nonnull
    public static List<Node<?>> buildResourceNodes() {
        final AzureResourceManager resourceManager = AzureResourceManager.getInstance();
        final List<AbstractAzResource<?, ?, ?>> resources = resourceManager.getResources().stream()
            .map(ResourceId::id)
            .map(String::toLowerCase).distinct()
            .parallel().map(id -> Azure.az().getById(id))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        final AzureExplorer.AzureExplorerNodeProviderManager manager = AzureExplorer.getManager();
        return resources.stream()
            .map(r -> manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
            .sorted(Comparator.comparing(Node::getLabel))
            .collect(Collectors.toList());
    }

    private static class RootNode extends TypeGroupedServicesRootNode {
        private static final String NAME = "Azure";

        protected void onAuthEvent() {
            final AzureAccount az = Azure.az(AzureAccount.class);
            String desc = "";
            if (az.isLoggingIn()) {
                desc = "Signing In...";
                this.withTips("Signing in...");
            } else if (!az.isLoggedIn()) {
                desc = "Not Signed In";
            }
            this.withDescription(" " + desc);
            this.refreshViewLater();
        }
    }
}
