/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EnableComboBox extends AzureComboBox<Boolean> {
    public EnableComboBox() {
        super(true);
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Boolean) {
            return (Boolean) item ? "Enabled" : "Disabled";
        }
        return super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected List<? extends Boolean> loadItems() {
        return Arrays.asList(Boolean.TRUE, Boolean.FALSE);
    }
}
