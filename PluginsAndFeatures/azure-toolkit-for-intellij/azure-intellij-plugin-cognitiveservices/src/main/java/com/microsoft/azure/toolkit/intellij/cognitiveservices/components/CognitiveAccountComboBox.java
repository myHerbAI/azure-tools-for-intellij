/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.collections4.ListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CognitiveAccountComboBox extends AzureComboBox<CognitiveAccount> {
    private Subscription subscription;
    private final List<CognitiveAccount> draftItems = new LinkedList<>();

    public CognitiveAccountComboBox() {
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

    @Override
    public void setValue(@Nullable CognitiveAccount account, Boolean fixed) {
        if (Objects.nonNull(account) && account.isDraftForCreating()) {
            this.draftItems.remove(account);
            this.draftItems.add(0, account);
            this.reloadItems();
        }
        super.setValue(account, fixed);
    }

    @Nonnull
    @Override
    protected List<? extends CognitiveAccount> loadItems()  {
        return Optional.ofNullable(subscription)
                .map(s -> {
                    final List<CognitiveAccount> list = Azure.az(AzureCognitiveServices.class).forSubscription(s.getId()).accounts().list();
                    return ListUtils.union(this.draftItems, list);
                })
                .orElse(Collections.emptyList());
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof CognitiveAccount) {
            final String name = ((CognitiveAccount) item).getName();
            return ((CognitiveAccount) item).isDraftForCreating() ? String.format("(New) %s", name) : name;
        }
        return super.getItemText(item);
    }
}
