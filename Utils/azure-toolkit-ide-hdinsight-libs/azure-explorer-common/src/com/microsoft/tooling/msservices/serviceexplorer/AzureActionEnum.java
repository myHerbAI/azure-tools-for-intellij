/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import lombok.Getter;

public enum AzureActionEnum {

    REFRESH("Refresh", "Refreshing", AzureIcons.Action.REFRESH, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY),
    CREATE("Create", "Creating", AzureIcons.Common.CREATE, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY + 2),

    OPEN_IN_PORTAL("Open In Portal", "Opening", AzureIcons.Common.OPEN_IN_PORTAL, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY + 10),
    OPEN_IN_BROWSER("Open In Browser", "Opening", AzureIcons.Common.OPEN_IN_PORTAL, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY + 11),
    SHOW_PROPERTIES("Show Properties", "Loading", AzureIcons.Common.SHOW_PROPERTIES),

    START("Start", "Starting", AzureIcons.Common.START, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 1),
    STOP("Stop", "Stopping", AzureIcons.Common.STOP, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 2),
    RESTART("Restart", "Restarting", AzureIcons.Common.RESTART, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 3),
    DELETE("Delete", "Deleting", AzureIcons.Common.DELETE, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 4);

    @Getter
    private final String name;
    @Getter
    private final String doingName;
    @Getter
    private final AzureIcon iconSymbol;
    @Getter
    private final Integer group;
    @Getter
    private final Integer priority;

    AzureActionEnum(String name, String doingName, AzureIcon iconSymbol) {
        this(name, doingName, iconSymbol, Groupable.DEFAULT_GROUP, Sortable.DEFAULT_PRIORITY);
    }

    AzureActionEnum(String name, String doingName, AzureIcon iconSymbol, Integer group, Integer priority) {
        this.name = name;
        this.doingName = doingName;
        this.iconSymbol = iconSymbol;
        this.group = group;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return name();
    }

}
