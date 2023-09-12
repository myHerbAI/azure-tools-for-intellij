/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import org.jetbrains.annotations.Nullable;

public class AzureSettingsConfigurableProvider extends ConfigurableProvider {

    @Override
    public boolean canCreateConfigurable() {
        return true;
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new AzureSettingsConfigurable();
    }
}
