/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.settings

import com.intellij.openapi.options.ConfigurableProvider

class AzureSettingsConfigurableProvider : ConfigurableProvider() {
    override fun canCreateConfigurable() = true
    override fun createConfigurable() = AzureSettingsConfigurable()
}