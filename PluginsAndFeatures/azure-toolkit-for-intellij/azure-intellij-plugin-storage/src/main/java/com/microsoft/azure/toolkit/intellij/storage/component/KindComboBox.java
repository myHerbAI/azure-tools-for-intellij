/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.component;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KindComboBox extends AzureComboBox<Kind> {

    private Performance performance;

    public void setPerformance(Performance performance) {
        if (Objects.equals(performance, this.performance)) {
            return;
        }
        this.performance = performance;
        if (performance == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nullable
    @Override
    protected Kind doGetDefaultValue() {
        return CacheManager.getUsageHistory(Kind.class)
            .peek(v -> Objects.isNull(performance) || Objects.equals(performance, v.getPerformance()));
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "storage|account.kind.list.supported",
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends Kind> loadItems() {
        return Objects.isNull(this.performance) ? Collections.emptyList() : Azure.az(AzureStorageAccount.class).listSupportedKinds(this.performance);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Kind ? ((Kind) item).getLabel() : super.getItemText(item);
    }
}
