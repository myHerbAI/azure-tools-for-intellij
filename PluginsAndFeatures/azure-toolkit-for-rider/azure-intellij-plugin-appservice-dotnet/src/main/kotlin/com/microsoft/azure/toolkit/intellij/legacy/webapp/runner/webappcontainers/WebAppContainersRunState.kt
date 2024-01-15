/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateOrUpdateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext

class WebAppContainersRunState(
    project: Project,
    private val webAppContainersConfiguration: WebAppContainersConfiguration
) : RiderAzureRunProfileState<AppServiceAppBase<*, *, *>>(project) {
    companion object {
        private const val WEBSITES_PORT = "WEBSITES_PORT"
    }

    override fun executeSteps(processHandler: RunProcessHandler): AppServiceAppBase<*, *, *> {
        OperationContext.current().setMessager(processHandlerMessenger)

        processHandler.setText("Start Web App for Containers deployment...")

        //push image


        //create web app
        val config = createDotNetAppServiceConfig()
        val task = CreateOrUpdateDotNetWebAppTask(config)
        return task.execute()
    }

    private fun createDotNetAppServiceConfig() =
        DotNetAppServiceConfig().apply {
            subscriptionId(webAppContainersConfiguration.subscriptionId)
            resourceGroup(webAppContainersConfiguration.resourceGroupName)
            region(Region.fromName(webAppContainersConfiguration.locationName))
            servicePlanName(webAppContainersConfiguration.appServicePlanName)
            servicePlanResourceGroup(webAppContainersConfiguration.appServicePlanResourceGroupName)
            pricingTier(
                PricingTier.fromString(
                    webAppContainersConfiguration.pricingSkuTier,
                    webAppContainersConfiguration.pricingSkuSize
                )
            )
            appName(webAppContainersConfiguration.webAppName)
            runtime(createRuntimeConfig())
            dotnetRuntime = createDotNetRuntimeConfig()
            appSettings(mapOf(WEBSITES_PORT to webAppContainersConfiguration.port.toString()))
        }

    private fun createRuntimeConfig() = RuntimeConfig().apply {
        os(OperatingSystem.DOCKER)
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
        image("${webAppContainersConfiguration.finalRepositoryName}:${webAppContainersConfiguration.finalTagName}")
    }

    private fun createDotNetRuntimeConfig() = DotNetRuntimeConfig().apply {
        os(OperatingSystem.LINUX)
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
        image("${webAppContainersConfiguration.finalRepositoryName}:${webAppContainersConfiguration.finalTagName}")
        isDocker = true
    }

    override fun onSuccess(result: AppServiceAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val imageRepository = webAppContainersConfiguration.finalRepositoryName
        val imageTag = webAppContainersConfiguration.finalTagName

        updateConfigurationDataModel(result)
        processHandler.setText("Image $imageRepository:$imageTag has been deployed to Web App ${result.name}")
        val url = "https://${result.name}.azurewebsites.net/"
        processHandler.setText("URL: $url")
        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: AppServiceAppBase<*, *, *>) {
        webAppContainersConfiguration.apply {
            isCreatingNewWebAppOnLinux = false
            webAppId = app.id
            resourceGroupName = app.resourceGroupName
        }
    }
}