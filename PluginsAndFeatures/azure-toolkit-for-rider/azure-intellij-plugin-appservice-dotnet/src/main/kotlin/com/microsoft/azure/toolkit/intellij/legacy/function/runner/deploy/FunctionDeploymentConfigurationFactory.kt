/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import javax.swing.Icon

class FunctionDeploymentConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    companion object {
        private const val FACTORY_ID = "Azure - Deploy Function"
        private const val FACTORY_NAME = "Deploy Function"
    }

    override fun getId() = FACTORY_ID

    override fun getIcon(): Icon = IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.DEPLOY)

    override fun getName() = FACTORY_NAME

    override fun createTemplateConfiguration(project: Project) =
        FunctionDeploymentConfiguration(project, this, project.name)

    override fun createConfiguration(name: String?, template: RunConfiguration) =
        FunctionDeploymentConfiguration(template.project, this, name)
}