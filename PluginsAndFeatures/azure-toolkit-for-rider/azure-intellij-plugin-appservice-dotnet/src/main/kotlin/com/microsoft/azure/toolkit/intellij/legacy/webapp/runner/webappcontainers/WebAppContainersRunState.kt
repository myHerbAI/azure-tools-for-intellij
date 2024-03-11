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
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
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

        val options = requireNotNull(webAppContainersConfiguration.state)

        //push image


        //create web app
        val config = createDotNetAppServiceConfig(options)
        val task = CreateOrUpdateDotNetWebAppTask(config)
        return task.execute()
    }

    private fun createDotNetAppServiceConfig(
        options: WebAppContainersConfigurationOptions
    ) = DotNetAppServiceConfig().apply {
        subscriptionId(options.subscriptionId)
        resourceGroup(options.resourceGroupName)
        region(Region.fromName(requireNotNull(options.region)))
        servicePlanName(options.appServicePlanName)
        servicePlanResourceGroup(options.appServicePlanResourceGroupName)
        val pricingTier = PricingTier(options.pricingTier, options.pricingSize)
        pricingTier(pricingTier)
        appName(options.webAppName)
        runtime(createRuntimeConfig(options))
        dotnetRuntime = createDotNetRuntimeConfig(options)
        appSettings(mapOf(WEBSITES_PORT to options.port.toString()))
    }

    private fun createRuntimeConfig(options: WebAppContainersConfigurationOptions) = RuntimeConfig().apply {
        os(OperatingSystem.DOCKER)
        image("${options.imageRepository}:${options.imageTag}")
    }

    private fun createDotNetRuntimeConfig(options: WebAppContainersConfigurationOptions) = DotNetRuntimeConfig().apply {
        os(OperatingSystem.LINUX)
        image("${options.imageRepository}:${options.imageTag}")
        isDocker = true
    }

    override fun onSuccess(result: AppServiceAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val options = requireNotNull(webAppContainersConfiguration.state)
        val imageRepository = options.imageRepository
        val imageTag = options.imageTag

        processHandler.setText("Image $imageRepository:$imageTag has been deployed to Web App ${result.name}")
        val url = "https://${result.name}.azurewebsites.net/"
        processHandler.setText("URL: $url")
        processHandler.notifyComplete()
    }
}