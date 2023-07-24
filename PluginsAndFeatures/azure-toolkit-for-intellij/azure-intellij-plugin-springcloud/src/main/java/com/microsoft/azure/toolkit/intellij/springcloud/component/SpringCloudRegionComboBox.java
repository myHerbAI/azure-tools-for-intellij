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

public class SpringCloudRegionComboBox extends AzureComboBox<Region> {

    @Nullable
    private Subscription subscription;
    @Nullable
    private Sku sku;

    @Override
    public String getLabel() {
        return "Region";
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        return ((Region) item).label();
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

    public void setSku(Sku sku) {
        if (Objects.equals(sku, this.sku)) {
            return;
        }
        this.sku = sku;
        if (sku == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nullable
    @Override
    protected Region doGetDefaultValue() {
        return CacheManager.getUsageHistory(Region.class).peek();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/springcloud.list_regions.subscription", params = {"this.subscription.getId()"})
    protected List<Region> loadItems() {
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.getId();
            final SpringCloudServiceSubscription az = Azure.az(AzureSpringCloud.class).forSubscription(sid);
            return az.listSupportedRegions(this.sku);
        }
        return Collections.emptyList();
    }
}
