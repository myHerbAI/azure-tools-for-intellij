/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateOrUpdateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetWebAppDeploymentSlotDraft
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppArtifactService
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.*
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import java.awt.Desktop
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL

class WebAppRunState(project: Project, private val webAppConfiguration: WebAppConfiguration) :
    RiderAzureRunProfileState<WebAppBase<*, *, *>>(project) {

    override fun executeSteps(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        OperationContext.current().setMessager(processHandlerMessenger)

        val publishableProjectPath = webAppConfiguration.publishableProjectPath
            ?: throw RuntimeException("Project is not defined")
        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { it.projectFilePath == publishableProjectPath }
            ?: throw RuntimeException("Project is not defined")

        val zipFile = WebAppArtifactService.getInstance(project)
            .prepareArtifact(
                publishableProject,
                webAppConfiguration.projectConfiguration,
                webAppConfiguration.projectPlatform,
                processHandler
            )

        val config = createDotNetAppServiceConfig(publishableProject)
        val createTask = CreateOrUpdateDotNetWebAppTask(config)
        val deployTarget = createTask.execute()

        val artifact = WebAppArtifact.builder()
            .file(zipFile)
            .deployType(DeployType.ZIP)
            .build()

        val deployTask = DeployWebAppTask(deployTarget, listOf(artifact), true, false, false)
        deployTask.execute()

        FileUtil.delete(zipFile)

        return deployTarget
    }

    private fun createDotNetAppServiceConfig(publishableProject: PublishableProjectModel) =
        DotNetAppServiceConfig().apply {
            subscriptionId(webAppConfiguration.subscriptionId)
            resourceGroup(webAppConfiguration.resourceGroup)
            region(Region.fromName(webAppConfiguration.region))
            servicePlanName(webAppConfiguration.appServicePlanName)
            servicePlanResourceGroup(webAppConfiguration.appServicePlanResourceGroupName)
            pricingTier(PricingTier.fromString(webAppConfiguration.pricing))
            appName(webAppConfiguration.webAppName)
            runtime(createRuntimeConfig())
            dotnetRuntime = createDotNetRuntimeConfig(publishableProject)
            appSettings(webAppConfiguration.applicationSettings)
            appSettingsToRemove(webAppConfiguration.appSettingsToRemove)
            deploymentSlotName(webAppConfiguration.newSlotName ?: webAppConfiguration.slotName)
            val configurationSource = when (webAppConfiguration.newSlotConfigurationSource) {
                "Don't clone configuration from an existing slot" -> DotNetWebAppDeploymentSlotDraft.CONFIGURATION_SOURCE_NEW
                webAppConfiguration.webAppName -> DotNetWebAppDeploymentSlotDraft.CONFIGURATION_SOURCE_PARENT
                else -> webAppConfiguration.newSlotConfigurationSource
            }
            deploymentSlotConfigurationSource(configurationSource)
        }

    private fun createRuntimeConfig() = RuntimeConfig().apply {
        os(webAppConfiguration.operatingSystem)
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
    }

    private fun createDotNetRuntimeConfig(publishableProject: PublishableProjectModel) = DotNetRuntimeConfig().apply {
        val operatingSystem = webAppConfiguration.operatingSystem
        os(operatingSystem)
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
        isDocker = false
        val stackAndVersion = publishableProject.getStackAndVersion(project, operatingSystem)
        stack = stackAndVersion?.first
        frameworkVersion = stackAndVersion?.second
    }

    override fun onSuccess(result: WebAppBase<*, *, *>, processHandler: RunProcessHandler) {
        updateConfigurationDataModel(result)
        processHandler.setText("Deployment was successful, but the app may still be starting.")
        val url = "https://${result.hostName}"
        processHandler.setText("URL: $url")
        if (webAppConfiguration.isOpenBrowserAfterDeployment) {
            openWebAppInBrowser(url, processHandler)
        }
        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: WebAppBase<*, *, *>) {
        webAppConfiguration.isCreatingNew = false
        if (app is WebAppDeploymentSlot) {
            webAppConfiguration.slotName = app.name
            webAppConfiguration.newSlotConfigurationSource = null
            webAppConfiguration.newSlotName = null
            webAppConfiguration.webAppId = app.parent.id
        } else {
            webAppConfiguration.webAppId = app.id
        }
        webAppConfiguration.applicationSettings = app.appSettings ?: emptyMap()
        webAppConfiguration.appSettingsToRemove = emptySet()
        webAppConfiguration.webAppName = app.name
        webAppConfiguration.resourceGroup = app.resourceGroupName
        webAppConfiguration.isCreatingAppServicePlan = false
        webAppConfiguration.appServicePlanName = app.getAppServicePlan()?.name
        webAppConfiguration.appServicePlanResourceGroupName = app.getAppServicePlan()?.resourceGroupName
    }

    private fun openWebAppInBrowser(url: String, processHandler: RunProcessHandler) {
        try {
            Desktop.getDesktop().browse(URL(url).toURI())
        } catch (ex: Exception) {
            when (ex) {
                is IOException,
                is URISyntaxException -> processHandler.println(ex.message, ProcessOutputTypes.STDERR)

                else -> throw ex
            }
        }
    }
}