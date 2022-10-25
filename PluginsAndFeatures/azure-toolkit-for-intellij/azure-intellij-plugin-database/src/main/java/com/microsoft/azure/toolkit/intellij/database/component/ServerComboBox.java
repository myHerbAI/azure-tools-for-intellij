/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class ServerComboBox<T extends IDatabaseServer<?>> extends AzureComboBox<T> {

    @Getter
    private Subscription subscription;

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

    @Nullable
    @Override
    protected T doGetDefaultValue() {
        final List<T> items = this.getItems();
        //noinspection unchecked
        return (T) CacheManager.getUsageHistory(items.get(0).getClass())
            .peek(v -> Objects.isNull(subscription) || Objects.equals(subscription, v.getSubscription()));
    }

    @Override
    protected String getItemText(Object item) {
        return Objects.nonNull(item) ? ((IDatabaseServer<?>) item).getName() : super.getItemText(item);
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public AzureValidationInfo doValidate(T server) {
        if (!StringUtils.equals("Ready", server.getStatus())) {
            return AzureValidationInfo.error("This server is not ready. please start it first.", this);
        }
        return AzureValidationInfo.success(this);
    }
}
