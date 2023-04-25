/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.component;

import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SubscriptionComboBox extends AzureComboBox<Subscription> {

    public SubscriptionComboBox(Composite parent) {
        super(parent, false);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/account.list_subscriptions")
    protected List<Subscription> loadItems() {
        try {
            return az(AzureAccount.class).account().getSelectedSubscriptions();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((Subscription) item).getName();
    }
}