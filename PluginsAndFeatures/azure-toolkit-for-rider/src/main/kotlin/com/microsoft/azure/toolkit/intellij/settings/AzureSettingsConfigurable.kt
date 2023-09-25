package com.microsoft.azure.toolkit.intellij.settings

import com.intellij.application.options.OptionsContainingConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class AzureSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {
    companion object {
        private const val AZURE_CONFIGURABLE_ID = "com.microsoft.intellij.RiderAzureConfigurable"
        private const val DISPLAY_NAME = "Azure"
    }

    private val azureSettingsPanel = AzureSettingsPanel()

    override fun getDisplayName() = DISPLAY_NAME
    override fun getId() = AZURE_CONFIGURABLE_ID

    override fun createComponent(): JComponent {
        azureSettingsPanel.init()
        return azureSettingsPanel.getPanel()
    }

    override fun isModified() = azureSettingsPanel.isModified()

    override fun apply() {
        azureSettingsPanel.apply()
    }

    override fun reset() {
        azureSettingsPanel.reset()
    }
}