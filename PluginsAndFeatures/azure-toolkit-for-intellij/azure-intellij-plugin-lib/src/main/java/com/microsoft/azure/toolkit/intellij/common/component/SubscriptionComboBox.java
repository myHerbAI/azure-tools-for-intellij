/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SubscriptionComboBox extends AzureComboBox<Subscription> {

    private final AzureEventBus.EventListener onSubscriptionsChanged;

    public SubscriptionComboBox() {
        super();
        this.onSubscriptionsChanged = new AzureEventBus.EventListener(e -> this.reloadItems());
    }

    @Override
    public String getLabel() {
        return "Subscription";
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/account.list_subscriptions")
    protected List<Subscription> loadItems() {
        return az(AzureAccount.class).account().getSelectedSubscriptions().stream()
            .sorted(Comparator.comparing(Subscription::getName))
            .collect(Collectors.toList());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        AzureEventBus.on("account.subscription_changed.account", this.onSubscriptionsChanged);
    }

    @Override
    public void removeNotify() {
        AzureEventBus.off("account.subscription_changed.account", this.onSubscriptionsChanged);
        super.removeNotify();
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((Subscription) item).getName();
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
