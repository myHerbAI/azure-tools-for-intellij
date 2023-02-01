/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.arm;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class DeploymentActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String DEPLOYMENT_ACTIONS = "actions.resourceDeployments.deployment";
    public static final String DEPLOYMENTS_ACTIONS = "actions.resourceDeployments.deployments";

    public static final Action.Id<ResourceDeployment> EDIT = Action.Id.of("user/arm.edit_deployment.deployment");
    public static final Action.Id<ResourceDeployment> UPDATE = Action.Id.of("user/arm.update_deployment.deployment");
    public static final Action.Id<ResourceDeployment> EXPORT_TEMPLATE = Action.Id.of("user/arm.export_template.deployment");
    public static final Action.Id<ResourceDeployment> EXPORT_PARAMETER = Action.Id.of("user/arm.export_parameter.deployment");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_DEPLOYMENT = Action.Id.of("user/arm.create_deployment.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(EDIT)
            .withLabel("Edit Deployment")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceDeployment)
            .enableWhen(s -> s.getFormalStatus().isWritable())
            .withShortcut(am.getIDEDefaultShortcuts().view())
            .register(am);

        new Action<>(EXPORT_TEMPLATE)
            .withLabel("Export Template File")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceDeployment)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(EXPORT_PARAMETER)
            .withLabel("Export Parameter File")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceDeployment)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(UPDATE)
            .withLabel("Update Deployment")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceDeployment)
            .enableWhen(s -> s.getFormalStatus().isWritable())
            .register(am);

        new Action<>(GROUP_CREATE_DEPLOYMENT)
            .withLabel("Deployment")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup deploymentsActions = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(DEPLOYMENTS_ACTIONS, deploymentsActions);

        final ActionGroup deploymentActions = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            DeploymentActionsContributor.EDIT,
            DeploymentActionsContributor.UPDATE,
            ResourceCommonActionsContributor.DELETE,
            "---",
            DeploymentActionsContributor.EXPORT_TEMPLATE,
            DeploymentActionsContributor.EXPORT_PARAMETER
        );
        am.registerGroup(DEPLOYMENT_ACTIONS, deploymentActions);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_DEPLOYMENT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
