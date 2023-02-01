/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.containerapps.model.TransportMethod;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class TransportMethodComboBox extends AzureComboBox<TransportMethod> {
    public TransportMethodComboBox() {
        super(true);
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof TransportMethod ? ((TransportMethod) item).getValue() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected List<? extends TransportMethod> loadItems() {
        return TransportMethod.values();
    }
}
