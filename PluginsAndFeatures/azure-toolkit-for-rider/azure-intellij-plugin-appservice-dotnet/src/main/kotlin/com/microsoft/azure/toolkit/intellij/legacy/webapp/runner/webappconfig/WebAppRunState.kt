/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppDeploymentSlotDraft
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateOrUpdateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.ArtifactService
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.intellij.legacy.getStackAndVersion
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import java.awt.Desktop
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL

class WebAppRunState(
    project: Project,
    private val webAppConfiguration: WebAppConfiguration
) : RiderAzureRunProfileState<WebAppBase<*, *, *>>(project) {

    override fun executeSteps(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        OperationContext.current().setMessager(processHandlerMessenger)

        processHandler.setText("Start Web App deployment...")

        val options = requireNotNull(webAppConfiguration.state)
        val publishableProjectPath = options.publishableProjectPath
            ?: throw RuntimeException("Project is not defined")
        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { it.projectFilePath == publishableProjectPath }
            ?: throw RuntimeException("Project is not defined")

        val config = createDotNetAppServiceConfig(publishableProject, options)
        val createTask = CreateOrUpdateDotNetWebAppTask(config)
        val deployTarget = createTask.execute()

        if (deployTarget is AzResource.Draft<*, *>) {
            deployTarget.reset()
        }
        webAppConfiguration.state?.apply {
            appSettings = deployTarget.appSettings ?: mutableMapOf()
        }

        val zipFile = ArtifactService.getInstance(project)
            .prepareArtifact(
                publishableProject,
                requireNotNull(options.projectConfiguration),
                requireNotNull(options.projectPlatform),
                processHandler,
                true
            )
        val artifact = WebAppArtifact.builder()
            .file(zipFile)
            .deployType(DeployType.ZIP)
            .build()

        val deployTask = DeployWebAppTask(deployTarget, listOf(artifact), true, false, false)
        deployTask.execute()

        FileUtil.delete(zipFile)

        return deployTarget
    }

    private fun createDotNetAppServiceConfig(
        publishableProject: PublishableProjectModel,
        options: WebAppConfigurationOptions
    ) = DotNetAppServiceConfig().apply {
        subscriptionId(options.subscriptionId)
        resourceGroup(options.resourceGroupName)
        region(Region.fromName(requireNotNull(options.region)))
        servicePlanName(options.appServicePlanName)
        servicePlanResourceGroup(options.appServicePlanResourceGroupName)
        val pricingTier = PricingTier(options.pricingTier, options.pricingSize)
        pricingTier(pricingTier)
        appName(options.webAppName)
        deploymentSlotName(options.slotName)
        val configurationSource = when (options.slotConfigurationSource) {
            "Do not clone settings" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_NEW
            "parent" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_PARENT
            null -> null
            else -> options.slotConfigurationSource
        }
        deploymentSlotConfigurationSource(configurationSource)
        val os = OperatingSystem.fromString(options.operatingSystem)
        dotnetRuntime = createDotNetRuntimeConfig(publishableProject, os)
        appSettings(options.appSettings)
    }

    private fun createDotNetRuntimeConfig(publishableProject: PublishableProjectModel, os: OperatingSystem) =
        DotNetRuntimeConfig().apply {
            os(os)
            isDocker = false
            val stackAndVersion = publishableProject.getStackAndVersion(project, os)
            stack = stackAndVersion?.first
            frameworkVersion = stackAndVersion?.second
        }

    override fun onSuccess(result: WebAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val options = requireNotNull(webAppConfiguration.state)

        updateConfigurationDataModel(result)
        processHandler.setText("Deployment was successful, but the app may still be starting.")
        val url = "https://${result.hostName}"
        processHandler.setText("URL: $url")
        if (options.openBrowser) {
            openWebAppInBrowser(url, processHandler)
        }
        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: WebAppBase<*, *, *>) {
        webAppConfiguration.state?.apply {
            if (app is WebAppDeploymentSlot) {
                slotName = app.name
                slotConfigurationSource = null
            }

            appSettings = app.appSettings ?: mutableMapOf()
        }
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