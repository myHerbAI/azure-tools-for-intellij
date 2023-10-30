@file:Suppress("CompanionObjectInExtension")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class WebAppConfigurationType : ConfigurationTypeBase(
    "AzureWebAppPublish",
    "Azure - Web App",
    "Azure Publish Web App configuration",
    IntelliJAzureIcons.getIcon(AzureIcons.WebApp.MODULE)
) {
    companion object {
        fun getInstance() = ConfigurationTypeUtil.findConfigurationType(WebAppConfigurationType::class.java)
    }

    init {
        addFactory(RiderWebAppConfigurationFactory(this))
    }

    fun getFactory() = RiderWebAppConfigurationFactory(this)
}