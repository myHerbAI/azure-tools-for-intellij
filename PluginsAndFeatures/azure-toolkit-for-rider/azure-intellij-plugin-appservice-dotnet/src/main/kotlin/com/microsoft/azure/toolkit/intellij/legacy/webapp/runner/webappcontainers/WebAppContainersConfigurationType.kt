/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("CompanionObjectInExtension")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class WebAppContainersConfigurationType : SimpleConfigurationType(
    "AzureWebAppContainersDeploy",
    "Azure - Web App for Containers",
    "Azure Publish Web App for Containers configuration",
    NotNullLazyValue.createValue { IntelliJAzureIcons.getIcon(AzureIcons.WebApp.DEPLOY) }
) {
    companion object {
        fun getInstance() = ConfigurationTypeUtil.findConfigurationType(WebAppContainersConfigurationType::class.java)
    }

    override fun createTemplateConfiguration(project: Project) =
        WebAppContainersConfiguration(project, this, project.name)

    override fun createConfiguration(name: String?, template: RunConfiguration) =
        WebAppContainersConfiguration(template.project, this, name)
}