/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerapps;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.*;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Revision;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.RevisionModule;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class ContainerAppsNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Container Apps";
    private static final String ICON = AzureIcons.ContainerApps.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureContainerApps.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureContainerApps || data instanceof ContainerAppsEnvironment ||
            data instanceof ContainerApp || data instanceof Revision;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureContainerApps) {
            final AzureContainerApps service = ((AzureContainerApps) data);
            final Function<AzureContainerApps, List<ContainerAppsEnvironment>> registries = asc -> asc.list().stream()
                .flatMap(m -> m.environments().list().stream())
                .collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(ContainerAppsActionsContributor.SERVICE_ACTIONS)
                .addChildren(registries, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof ContainerAppsEnvironment) {
            final ContainerAppsEnvironment environment = (ContainerAppsEnvironment) data;
            return new Node<>(environment)
                .view(new ContainerAppsEnvironmentLabelView(environment))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addChildren(ContainerAppsEnvironment::listContainerApps, (app, envNode) -> this.createNode(app, envNode, manager))
                .actions(ContainerAppsActionsContributor.ENVIRONMENT_ACTIONS);
        } else if (data instanceof ContainerApp) {
            final ContainerApp app = (ContainerApp) data;
            return new Node<>(app)
                .view(new AzureResourceLabelView<>(app))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addChildren(ContainerApp::getSubModules, (revision, appNode) -> this.createNode(revision, appNode, manager))
                .hasMoreChildren(a -> a.revisions().hasMoreResources())
                .loadMoreChildren(a -> a.revisions().loadMoreResources())
                .actions(ContainerAppsActionsContributor.CONTAINER_APP_ACTIONS);
        } else if (data instanceof RevisionModule) {
            final RevisionModule module = (RevisionModule) data;
            return new Node<>(module)
                    .view(new AzureModuleLabelView<>(module, "Revisions", AzureIcons.ContainerApps.REVISION_MODULE.getIconPath()))
                    .actions(ContainerAppsActionsContributor.REVISION_MODULE_ACTIONS)
                    .addChildren(RevisionModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof Revision) {
            final Revision revision = (Revision) data;
            return new Node<>(revision)
                .view(new AzureResourceLabelView<>(revision, this::getRevisionDescription, DEFAULT_AZURE_RESOURCE_ICON_PROVIDER))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .actions(ContainerAppsActionsContributor.REVISION_ACTIONS);
        } else if (data instanceof ServiceLinkerModule) {
            final ServiceLinkerModule module = (ServiceLinkerModule) data;
            return new Node<>(module)
                    .view(new AzureModuleLabelView<>(module, "Service Connector", AzureIcons.Connector.SERVICE_LINKER_MODULE.getIconPath()))
                    .actions(ResourceCommonActionsContributor.SERVICE_LINKER_MODULE_ACTIONS)
                    .addChildren(ServiceLinkerModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof ServiceLinker) {
            final ServiceLinker serviceLinker = (ServiceLinker) data;
            return new ServiceLinkerNode(serviceLinker);
        }
        return null;
    }

    private String getRevisionDescription(@Nonnull final Revision revision) {
        final AzResource.FormalStatus formalStatus = revision.getFormalStatus();
        if (!formalStatus.isRunning() && !formalStatus.isStopped()) {
            return revision.getStatus();
        }
        final Revision latestRevision = revision.getParent().getLatestRevision();
        final boolean latest = Objects.equals(latestRevision, revision);
        return String.format(latest ? "%s (Latest)" : "%s", revision.isActive() ? "Active" : "Inactive");
    }

    static class ContainerAppsEnvironmentLabelView extends AzureResourceLabelView<ContainerAppsEnvironment> {
        public ContainerAppsEnvironmentLabelView(@NotNull ContainerAppsEnvironment resource) {
            super(resource);
        }

        @Override
        public void onEvent(AzureEvent event) {
            final String type = event.getType();
            final Object source = event.getSource();
            if (source instanceof AzureContainerAppsServiceSubscription
                    && StringUtils.equals(((AzureContainerAppsServiceSubscription) source).getSubscriptionId(), resource.getSubscriptionId())
                    && StringUtils.equals(type, "resource.children_changed.resource")) {
                AzureTaskManager.getInstance().runLater(() -> this.refreshChildren(true));
            }
            super.onEvent(event);
        }
    }
}
