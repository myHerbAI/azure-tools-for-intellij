/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.cognitiveservices;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class CognitiveServicesNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Azure OpenAI";
    private static final String ICON = AzureIcons.CognitiveServices.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureCognitiveServices.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureCognitiveServices || data instanceof CognitiveAccount || data instanceof CognitiveDeployment;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureCognitiveServices) {
            final Function<AzureCognitiveServices, List<CognitiveAccount>> accountsFunction = asc -> asc.list().stream()
                .flatMap(m -> m.accounts().list().stream())
                .collect(Collectors.toList());
            return new AzServiceNode<>((AzureCognitiveServices) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(CognitiveServicesActionsContributor.SERVICE_ACTIONS)
                .addChildren(accountsFunction, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof CognitiveAccount) {
            return new AzResourceNode<>((CognitiveAccount) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addChildren(account -> account.deployments().list(), (deployment, accountNode) -> this.createNode(deployment, accountNode, manager))
                .withActions(CognitiveServicesActionsContributor.ACCOUNT_ACTIONS)
                .withMoreChildren(a -> a.deployments().hasMoreResources(), a -> a.deployments().loadMoreResources());
        } else if (data instanceof CognitiveDeployment) {
            return new AzResourceNode<>((CognitiveDeployment) data)
                .withDescription(deployment -> String.format("%s (version: %s)", deployment.getModel().getName(), deployment.getModel().getVersion()))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked((d, e) -> {
                    final AzureActionManager am = AzureActionManager.getInstance();
                    if (d.getModel().isGPTModel()) {
                        am.getAction(CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND).handle(d, e);
                    } else {
                        am.getAction(ResourceCommonActionsContributor.OPEN_PORTAL_URL).handle(d, e);
                    }
                })
                .withActions(CognitiveServicesActionsContributor.DEPLOYMENT_ACTIONS);
        }
        return null;
    }
}
