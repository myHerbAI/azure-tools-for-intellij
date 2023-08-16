/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountSku;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CognitiveAccountSkuComboBox extends AzureComboBox<AccountSku> {
    private Subscription subscription;
    private Region region;

    public CognitiveAccountSkuComboBox() {
        super(false);
    }

    public void setSubscription(@Nullable final Subscription subscription) {
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

    public void setRegion(@Nullable final Region region) {
        if (Objects.equals(region, this.region)) {
            return;
        }
        this.region = region;
        this.reloadItems();
    }

    @Nonnull
    @Override
    protected List<? extends AccountSku> loadItems() throws Exception {
        return Optional.ofNullable(subscription)
                .map(s -> Azure.az(AzureCognitiveServices.class).forSubscription(s.getId()).accounts().listSkus(region))
                .orElse(Collections.emptyList());
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof AccountSku ? String.format("%s %s", ((AccountSku) item).getTier(), ((AccountSku) item).getName()) : super.getItemText(item);
    }
}
