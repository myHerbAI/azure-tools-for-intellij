/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServerDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CreateMySqlAction {

    public static void create(@Nonnull Project project, @Nullable DatabaseServerConfig data) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final MySqlCreationDialog dialog = new MySqlCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            final Action.Id<DatabaseServerConfig> actionId = Action.Id.of("user/mysql.create_server.server");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(DatabaseServerConfig::getName)
                .withSource(DatabaseServerConfig::getResourceGroup)
                .withAuthRequired(true)
                .withHandler(draft -> doCreate(draft, project)));
            dialog.show();
        });

    }

    private static void doCreate(final DatabaseServerConfig config, final Project project) {
        final ResourceGroup rg = config.getResourceGroup();
        if (Objects.nonNull(rg) && rg.isDraftForCreating()) {
            new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
        }
        final MySqlServerDraft draft = Azure.az(AzureMySql.class).servers(config.getSubscription().getId())
            .create(config.getName(), config.getResourceGroup().getName());
        draft.setConfig(config);
        draft.commit();
        CacheManager.getUsageHistory(MySqlServer.class).push(draft);
    }
}
