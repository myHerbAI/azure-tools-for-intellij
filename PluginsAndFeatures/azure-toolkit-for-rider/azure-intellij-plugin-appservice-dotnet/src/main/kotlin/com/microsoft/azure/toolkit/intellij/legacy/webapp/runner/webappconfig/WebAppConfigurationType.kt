/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

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
        addFactory(WebAppConfigurationFactory(this))
    }

    fun getFactory() = WebAppConfigurationFactory(this)
}