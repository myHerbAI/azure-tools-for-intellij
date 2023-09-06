/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class ContainerRegistryModule extends AzureRefreshableNode {

    private static final String ACR_MODULE_ID = ContainerRegistryModule.class.getName();
    private static final String ICON_PATH = "acr.png";
    private static final String BASE_MODULE_NAME = "Container Registries";
    public static final String MODULE_NAME = "Container Registry";

    /**
     * The root node for ACR resource.
     */
    public ContainerRegistryModule(Node parent) {
        super(ACR_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    @Override
    public @Nullable AzureIcon getIconSymbol() {
        return AzureIcons.ContainerRegistry.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        final List<ContainerRegistry> registryList = ContainerRegistryMvpModel.getInstance().listContainerRegistries();
        registryList.forEach(app -> addChildNode(new ContainerRegistryNode(this, app.getSubscriptionId(), app.getId(), app.getName())));
    }
}
