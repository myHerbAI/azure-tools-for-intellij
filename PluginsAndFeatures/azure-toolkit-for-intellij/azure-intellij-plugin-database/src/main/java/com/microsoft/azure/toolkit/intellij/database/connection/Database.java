/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Database {
    private final ResourceId serverId;
    private final String name;

    private JdbcUrl jdbcUrl;
    private String username;
    private char[] password;

    public Database(String serverId, String name) {
        this.serverId = ResourceId.fromString(serverId);
        this.name = name;
    }

    public Database(String id) {
        final ResourceId dbId = ResourceId.fromString(id);
        this.serverId = dbId.parent();
        this.name = dbId.name();
    }

    public String getFullName() {
        return this.getServerName() + "/" + this.getName();
    }

    public String getServerName() {
        return this.serverId.name();
    }

    @EqualsAndHashCode.Include
    public String getId() {
        return this.serverId.id() + "/databases/" + name;
    }
}
