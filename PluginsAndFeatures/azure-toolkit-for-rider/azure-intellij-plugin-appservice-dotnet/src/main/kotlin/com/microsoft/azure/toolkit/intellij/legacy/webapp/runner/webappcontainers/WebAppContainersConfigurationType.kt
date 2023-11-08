/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("CompanionObjectInExtension")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class WebAppContainersConfigurationType : ConfigurationTypeBase(
    "AzureWebAppContainersPublish",
    "Azure - Web App for Containers",
    "Azure Publish Web App for Containers configuration",
    IntelliJAzureIcons.getIcon(AzureIcons.WebApp.MODULE)
) {
    companion object {
        fun getInstance() = ConfigurationTypeUtil.findConfigurationType(WebAppContainersConfigurationType::class.java)
    }

    init {
        addFactory(WebAppContainersConfigurationFactory(this))
    }

    fun getFactory() = WebAppContainersConfigurationFactory(this)
}