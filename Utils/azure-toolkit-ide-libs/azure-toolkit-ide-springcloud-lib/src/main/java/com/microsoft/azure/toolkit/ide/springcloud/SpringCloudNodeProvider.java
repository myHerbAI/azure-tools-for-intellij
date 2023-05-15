/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.*;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import com.microsoft.azure.toolkit.lib.springcloud.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SpringCloudNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "Spring Apps";
    private static final String ICON = AzureIcons.SpringCloud.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureSpringCloud.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureSpringCloud ||
            data instanceof SpringCloudCluster ||
            data instanceof SpringCloudApp ||
            data instanceof SpringCloudAppInstance;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureSpringCloud) {
            final AzureSpringCloud service = (AzureSpringCloud) data;
            final Function<AzureSpringCloud, List<SpringCloudCluster>> clusters = asc -> asc.list().stream().flatMap(m -> m.clusters().list().stream())
                .collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, "Spring Apps", ICON))
                .actions(SpringCloudActionsContributor.SERVICE_ACTIONS)
                .addChildren(clusters, (cluster, ascNode) -> this.createNode(cluster, ascNode, manager));
        } else if (data instanceof SpringCloudCluster) {
            final SpringCloudCluster cluster = (SpringCloudCluster) data;
            return new Node<>(cluster)
                .view(new AzureResourceLabelView<>(cluster))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .actions(SpringCloudActionsContributor.CLUSTER_ACTIONS)
                .addChildren(c -> c.apps().list(), (app, clusterNode) -> this.createNode(app, clusterNode, manager))
                .hasMoreChildren(c -> c.apps().hasMoreResources())
                .loadMoreChildren(c -> c.apps().loadMoreResources());
        } else if (data instanceof SpringCloudApp) {
            final SpringCloudApp app = (SpringCloudApp) data;
            return new Node<>(app)
                .view(new AzureResourceLabelView<>(app))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addInlineAction(ResourceCommonActionsContributor.DEPLOY)
                .doubleClickAction(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .actions(SpringCloudActionsContributor.APP_ACTIONS)
                .addChildren(c -> Optional.ofNullable(c.getActiveDeployment()).map(SpringCloudDeployment::getSubModules).orElse(Collections.emptyList()),
                    (instanceModule, moduleNode) -> this.createNode(instanceModule, moduleNode, manager));
        } else if (data instanceof SpringCloudAppInstanceModule) {
            final SpringCloudAppInstanceModule module = (SpringCloudAppInstanceModule) data;
            return new Node<>(module)
                    .view(new AzureModuleLabelView<>(module, "Instances", AzureIcons.SpringCloud.INSTANCE_MODULE.getIconPath()))
                    .actions(SpringCloudActionsContributor.APP_INSTANCE_MODULE_ACTIONS)
                    .addChildren(SpringCloudAppInstanceModule::list, (d, p) -> this.createNode(d, p, manager));
        }
        else if (data instanceof SpringCloudAppInstance) {
            final SpringCloudAppInstance appInstance = (SpringCloudAppInstance) data;
            return new Node<>(appInstance)
                .view(new AzureResourceLabelView<>(appInstance))
                .actions(SpringCloudActionsContributor.APP_INSTANCE_ACTIONS);
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
}
