/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.file;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class AppServiceFileActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String APP_SERVICE_FILE_ACTIONS = "actions.appservice.file";
    public static final String APP_SERVICE_DIRECTORY_ACTIONS = "actions.appservice.directory";

    public static final Action.Id<AppServiceFile> APP_SERVICE_DIRECTORY_REFRESH = Action.Id.of("appservice.refresh_directory");
    public static final Action.Id<AppServiceFile> APP_SERVICE_FILE_VIEW = Action.Id.of("appservice.view_file");
    public static final Action.Id<AppServiceFile> APP_SERVICE_FILE_DOWNLOAD = Action.Id.of("appservice.download_file");

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup directoryActions = new ActionGroup(
                APP_SERVICE_DIRECTORY_REFRESH
        );
        am.registerGroup(APP_SERVICE_DIRECTORY_ACTIONS, directoryActions);

        final ActionGroup fileActions = new ActionGroup(
                APP_SERVICE_FILE_VIEW,
                APP_SERVICE_FILE_DOWNLOAD
        );
        am.registerGroup(APP_SERVICE_FILE_ACTIONS, fileActions);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<AppServiceFile> refresh = file -> AzureEventBus.emit("resource.refreshed.resource", file);
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", AzureIcons.Action.REFRESH.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("resource.refresh.resource", ((AppServiceFile) r).getName())).orElse(null))
                .enabled(s -> s instanceof AppServiceFile);
        final Action<AppServiceFile> action = new Action<>(APP_SERVICE_DIRECTORY_REFRESH, refresh, refreshView);
        action.setShortcuts(am.getIDEDefaultShortcuts().refresh());
        am.registerAction(APP_SERVICE_DIRECTORY_REFRESH, action);
    }

    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
