/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.mysql.single;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.mysql.single.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.single.MySqlServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class MySqlNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Azure Database for MySQL (On retire path)";
    private static final String ICON = AzureIcons.MySQL.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureMySql.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureMySql || data instanceof MySqlServer;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureMySql) {
            final Function<AzureMySql, List<MySqlServer>> servers = s -> s.list().stream()
                .flatMap(m -> m.servers().list().stream()).collect(Collectors.toList());
            return new AzServiceNode<>((AzureMySql) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .enableWhen(s -> false)
                .withActions(MySqlActionsContributor.SERVICE_ACTIONS)
                .addChildren(servers, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof MySqlServer) {
            return new AzResourceNode<>((MySqlServer) data)
                .withActions(MySqlActionsContributor.SERVER_ACTIONS);
        }
        return null;
    }
}
