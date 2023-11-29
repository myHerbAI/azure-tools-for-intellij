/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.run.configurations.publishing.RiderContextPublishProvider
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppConfiguration
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppConfigurationType
import javax.swing.Icon

class WebAppContextPublishProvider : RiderContextPublishProvider {
    override val icon: Icon
        get() = IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)
    override val name: String
        get() = "Publish to Azure"

    override fun getConfigurationForNode(
        project: Project,
        entity: ProjectModelEntity
    ): Pair<RunConfiguration, ConfigurationFactory> {
        val projectData = RiderContextPublishProvider.getProjectDataRecursive(project, entity)
            ?: error("Unexpected project node type. Cannot get project data for entity ${entity.url}")

        val factory =
            ConfigurationTypeUtil.findConfigurationType(WebAppConfigurationType::class.java).configurationFactories.single()
        val configuration =
            WebAppConfiguration(project, factory, "Publish ${projectData.value.projectName} to Azure")

        configuration.publishableProject = projectData.value

        return Pair(configuration, factory)
    }

    private fun isProjectPublishable(projectData: PublishableProjectModel?) =
        projectData != null && (projectData.isWeb && (projectData.isDotNetCore || SystemInfo.isWindows))

    override fun isAvailable(project: Project, entity: ProjectModelEntity): Boolean {
        val projectData = RiderContextPublishProvider.getProjectData(project, entity)
        return isProjectPublishable(projectData?.value)
    }

    override fun solutionHasAnyPublishableProjects(project: Project) =
        project.solution.publishableProjectsModel.publishableProjects.entries.any { isProjectPublishable(it.value) }
}