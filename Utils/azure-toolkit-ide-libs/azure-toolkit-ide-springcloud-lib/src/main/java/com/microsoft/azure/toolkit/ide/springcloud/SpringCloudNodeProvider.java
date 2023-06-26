/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.ServiceLinkerNode;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstanceModule;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;

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
            return new AzServiceNode<>((AzureSpringCloud) data)
                .withIcon(ICON).withLabel("Spring Apps")
                .withActions(SpringCloudActionsContributor.SERVICE_ACTIONS)
                .addChildren(clusters, (cluster, ascNode) -> this.createNode(cluster, ascNode, manager));
        } else if (data instanceof SpringCloudCluster) {
            final SpringCloudCluster cluster = (SpringCloudCluster) data;
            return new AzResourceNode<>(cluster)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(SpringCloudActionsContributor.CLUSTER_ACTIONS)
                .addChildren(c -> c.apps().list(), (app, clusterNode) -> this.createNode(app, clusterNode, manager))
                .withMoreChildren(c -> c.apps().hasMoreResources(), c -> c.apps().loadMoreResources());
        } else if (data instanceof SpringCloudApp) {
            final SpringCloudApp app = (SpringCloudApp) data;
            return new AzResourceNode<>(app)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addInlineAction(ResourceCommonActionsContributor.DEPLOY)
                .onDoubleClicked(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .withActions(SpringCloudActionsContributor.APP_ACTIONS)
                .addChildren(c -> Optional.ofNullable(c.getActiveDeployment()).map(SpringCloudDeployment::getSubModules).orElse(Collections.emptyList()),
                    (instanceModule, moduleNode) -> this.createNode(instanceModule, moduleNode, manager));
        } else if (data instanceof SpringCloudAppInstanceModule) {
            final SpringCloudAppInstanceModule module = (SpringCloudAppInstanceModule) data;
            return new AzModuleNode<>(module)
                .withIcon(AzureIcons.SpringCloud.INSTANCE_MODULE)
                .withLabel("Instances")
                .withActions(SpringCloudActionsContributor.APP_INSTANCE_MODULE_ACTIONS)
                .addChildren(SpringCloudAppInstanceModule::list, (d, p) -> this.createNode(d, p, manager));
        }
        else if (data instanceof SpringCloudAppInstance) {
            final SpringCloudAppInstance appInstance = (SpringCloudAppInstance) data;
            return new AzResourceNode<>(appInstance)
                .withActions(SpringCloudActionsContributor.APP_INSTANCE_ACTIONS);
        } else if (data instanceof ServiceLinkerModule) {
            final ServiceLinkerModule module = (ServiceLinkerModule) data;
            return new AzModuleNode<>(module)
                .withIcon(AzureIcons.Connector.SERVICE_LINKER_MODULE)
                .withLabel("Service Connector")
                .withActions(ResourceCommonActionsContributor.SERVICE_LINKER_MODULE_ACTIONS)
                .addChildren(ServiceLinkerModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof ServiceLinker) {
            final ServiceLinker serviceLinker = (ServiceLinker) data;
            return new ServiceLinkerNode(serviceLinker);
        }
        return null;
    }
}
