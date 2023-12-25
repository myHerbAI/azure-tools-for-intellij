/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.mysql;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class MySqlActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.mysql.service";
    public static final String SERVER_ACTIONS = "actions.mysql.server";

    private static final String NAME_PREFIX = "MySQL Server - %s";
    public static final Action.Id<AzResource> OPEN_DATABASE_TOOL = Action.Id.of("user/mysql.open_database_tools.server");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_MYSQL = Action.Id.of("user/mysql.create_server.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_DATABASE_TOOL)
            .visibleWhen(s -> s instanceof MySqlServer)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .withIcon(AzureIcons.Action.OPEN_DATABASE_TOOL.getIconPath())
            .withLabel("Open with Database Tools")
            .withIdParam(AzResource::getName)
            .register(am);

        new Action<>(GROUP_CREATE_MYSQL)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withLabel("MySQL server")
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
            MySqlActionsContributor.OPEN_DATABASE_TOOL,
            ResourceCommonActionsContributor.CONNECT,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SERVER_ACTIONS, serverActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_MYSQL);
    }
}
