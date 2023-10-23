@file:Suppress("CompanionObjectInExtension")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.RiderWebAppConfigurationFactory
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers.WebAppContainersConfigurationFactory

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
        addFactory(WebAppContainersConfigurationFactory(this))
    }

    fun getWebAppConfigurationFactory() = RiderWebAppConfigurationFactory(this)

    fun getWebAppContainersConfigurationFactory() = WebAppContainersConfigurationFactory(this)
}