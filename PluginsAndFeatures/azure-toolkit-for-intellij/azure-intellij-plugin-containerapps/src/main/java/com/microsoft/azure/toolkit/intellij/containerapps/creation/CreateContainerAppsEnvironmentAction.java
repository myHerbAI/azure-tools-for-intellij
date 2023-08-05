/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class CreateContainerAppsEnvironmentAction {
    public static void create(@Nullable Project project, @Nullable ContainerAppsEnvironmentDraft.Config data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ContainerAppsEnvironmentCreationDialog dialog = new ContainerAppsEnvironmentCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            final Action.Id<ContainerAppsEnvironmentDraft.Config> actionId = Action.Id.of("user/containerapps.create_container_apps_environment.environment");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(ContainerAppsEnvironmentDraft.Config::getName)
                .withSource(ContainerAppsEnvironmentDraft.Config::getResourceGroup)
                .withAuthRequired(true)
                .withHandler(c -> doCreate(c, project)));
            dialog.show();
        });
    }

    private static void doCreate(final ContainerAppsEnvironmentDraft.Config config, final Project project) {
        final ResourceGroup rg = config.getResourceGroup();
        if (rg.isDraftForCreating()) {
            new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
        }
        final ContainerAppsEnvironmentModule module = az(AzureContainerApps.class).environments(config.getSubscription().getId());
        final ContainerAppsEnvironmentDraft draft = module.create(config.getName(), config.getResourceGroup().getName());
        CacheManager.getUsageHistory(ContainerAppsEnvironment.class).push(draft);
        draft.setConfig(config);
        draft.commit();
    }
}
