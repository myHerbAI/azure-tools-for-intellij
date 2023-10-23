@file:Suppress("CompanionObjectInExtension")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.RiderWebAppConfigurationFactory
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers.WebAppContainersConfigurationFactory

class RiderWebAppConfigurationType : ConfigurationTypeBase(
    "AzureDotNetWebAppPublish",
    "Azure - Publish Web App",
    "Azure Publish Web App configuration",
    IntelliJAzureIcons.getIcon(AzureIcons.WebApp.DEPLOY)
) {
    companion object {
        fun getInstance() = ConfigurationTypeUtil.findConfigurationType(RiderWebAppConfigurationType::class.java)
    }

    init {
        addFactory(RiderWebAppConfigurationFactory(this))
        addFactory(WebAppContainersConfigurationFactory(this))
    }

    fun getWebAppConfigurationFactory() = RiderWebAppConfigurationFactory(this)

    fun getWebAppContainersConfigurationFactory() = WebAppContainersConfigurationFactory(this)
}