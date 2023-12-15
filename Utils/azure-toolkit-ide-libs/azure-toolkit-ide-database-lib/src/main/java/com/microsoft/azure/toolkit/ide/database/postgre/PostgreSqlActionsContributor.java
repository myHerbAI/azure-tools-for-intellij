/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.postgre;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class PostgreSqlActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.postgre.service";
    public static final String SERVER_ACTIONS = "actions.postgre.server";

    private static final String NAME_PREFIX = "PostgreSQL Server - %s";
    public static final Action.Id<AzResource> OPEN_DATABASE_TOOL = Action.Id.of("user/postgre.open_database_tools.server");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_POSTGRE = Action.Id.of("user/postgre.create_server.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_DATABASE_TOOL)
            .withLabel("Open with Database Tools")
            .withIcon(AzureIcons.Action.OPEN_DATABASE_TOOL.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof PostgreSqlServer)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(GROUP_CREATE_POSTGRE)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withLabel("PostgreSQL Server")
            .withIdParam(AzResource::getName)
            .register(am);
    }

    public int getOrder() {
        return INITIALIZE_ORDER;
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup serverActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            PostgreSqlActionsContributor.OPEN_DATABASE_TOOL,
            ResourceCommonActionsContributor.CONNECT,
            "---",
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SERVER_ACTIONS, serverActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_POSTGRE);
    }
}
