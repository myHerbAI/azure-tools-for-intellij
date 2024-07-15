/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.identities.AzureManagedIdentity;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class UserAssignedManagedIdentityComboBox extends AzureComboBox<Identity> {
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

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Identity) {
            return ((Identity) item).getName();
        }
        return super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<Identity> loadItems() throws Exception {
        if (Objects.isNull(subscription)) {
            return Azure.az(AzureManagedIdentity.class).identities();
        }
       return Azure.az(AzureManagedIdentity.class).forSubscription(this.subscription.getId()).identity().list();
    }
}
