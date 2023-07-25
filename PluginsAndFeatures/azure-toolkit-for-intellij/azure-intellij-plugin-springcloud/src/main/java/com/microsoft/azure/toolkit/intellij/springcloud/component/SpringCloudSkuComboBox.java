/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.azure.core.management.Region;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudServiceSubscription;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SpringCloudSkuComboBox extends AzureComboBox<Sku> {

    @Nullable
    private Subscription subscription;
    @Nullable
    private Region region;

    @Override
    public String getLabel() {
        return "Plan";
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        final Sku sku = (Sku) item;
        return sku.getLabel();
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    public void setRegion(Region region) {
        if (Objects.equals(region, this.region)) {
            return;
        }
        this.region = region;
        if (region == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nullable
    @Override
    protected Sku doGetDefaultValue() {
        return CacheManager.getUsageHistory(Sku.class).peek();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/springcloud.list_skus.subscription", params = {"this.subscription.getId()"})
    protected List<Sku> loadItems() {
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.getId();
            final SpringCloudServiceSubscription az = Azure.az(AzureSpringCloud.class).forSubscription(sid);
            return az.listSupportedSkus(this.region);
        }
        return Collections.emptyList();
    }
}
