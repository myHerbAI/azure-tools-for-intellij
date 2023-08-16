/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServerDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CreatePostgreSqlAction {
    public static void create(@Nonnull Project project, @Nullable DatabaseServerConfig data) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final PostgreSqlCreationDialog dialog = new PostgreSqlCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            final Action.Id<DatabaseServerConfig> actionId = Action.Id.of("user/postgre.create_server.server");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(DatabaseServerConfig::getName)
                .withAuthRequired(true)
                .withSource(DatabaseServerConfig::getResourceGroup)
                .withHandler(CreatePostgreSqlAction::doCreate));
            dialog.show();
        });

    }

    private static void doCreate(final DatabaseServerConfig config) {
        final ResourceGroup rg = config.getResourceGroup();
        if (Objects.nonNull(rg) && rg.isDraftForCreating()) {
            new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
        }
        final PostgreSqlServerDraft draft = Azure.az(AzurePostgreSql.class)
            .servers(config.getSubscription().getId())
            .create(config.getName(), rg.getName());
        draft.setConfig(config);
        draft.commit();
        CacheManager.getUsageHistory(PostgreSqlServer.class).push(draft);
    }
}
