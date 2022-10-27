/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImageOffer;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImagePublisher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ImageOfferComboBox extends AzureComboBox<VmImageOffer> {
    private VmImagePublisher publisher;

    public void setPublisher(VmImagePublisher publisher) {
        this.publisher = publisher;
        this.clear();
        reloadItems();
    }

    @Nullable
    @Override
    protected VmImageOffer doGetDefaultValue() {
        return CacheManager.getUsageHistory(VmImageOffer.class)
            .peek(v -> Objects.isNull(publisher) || Objects.equals(publisher, v.getPublisher()));
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof VmImageOffer ? ((VmImageOffer) item).name() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends VmImageOffer> loadItems() throws Exception {
        return Optional.ofNullable(publisher).map(VmImagePublisher::offers).orElse(Collections.emptyList());
    }
}
