/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import com.microsoft.azure.toolkit.redis.RedisCacheDraft;
import com.microsoft.azure.toolkit.redis.RedisCacheModule;
import com.microsoft.azure.toolkit.redis.model.RedisConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CreateRedisCacheAction {
    private static final String ERROR_CREATING_REDIS_CACHE = "Error creating Redis cache";

    public static void create(@Nonnull Project project, @Nullable RedisConfig data) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final RedisCreationDialog dialog = new RedisCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            dialog.setOkActionListener(config -> {
                doCreate(config, project);
                dialog.close();
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "user/redis.create_redis.redis", params = {"config.getName()"})
    private static void doCreate(final RedisConfig config, final Project project) {
        final AzureString title = OperationBundle.description("user/redis.create_redis.redis", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ResourceGroup rg = config.getResourceGroup();
            if (rg.isDraftForCreating()) {
                new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
            }
            final RedisCacheModule caches = Azure.az(AzureRedis.class).caches(config.getSubscription().getId());
            final RedisCacheDraft draft = caches.create(config.getName(), config.getResourceGroup().getName());
            draft.setPricingTier(config.getPricingTier());
            draft.setRegion(config.getRegion());
            draft.setNonSslPortEnabled(config.isEnableNonSslPort());
            draft.commit();
            CacheManager.getUsageHistory(RedisCache.class).push(draft);
        });
    }
}
