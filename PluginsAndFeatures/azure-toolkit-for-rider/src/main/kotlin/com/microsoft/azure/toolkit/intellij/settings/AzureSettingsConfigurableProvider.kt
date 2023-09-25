package com.microsoft.azure.toolkit.intellij.settings

import com.intellij.openapi.options.ConfigurableProvider

class AzureSettingsConfigurableProvider : ConfigurableProvider() {
    override fun canCreateConfigurable() = true
    override fun createConfigurable() = AzureSettingsConfigurable()
}