/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.ExecutionException
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.CreateOrUpdateDotNetFunctionAppTask
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DeployDotNetFunctionAppTask
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppDeploymentSlotDraft
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.ArtifactService
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.intellij.legacy.getFunctionStack
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext

class FunctionDeploymentState(
    project: Project,
    private val functionDeploymentConfiguration: FunctionDeploymentConfiguration
) : RiderAzureRunProfileState<FunctionAppBase<*, *, *>>(project) {
    companion object {
        private const val SCM_DO_BUILD_DURING_DEPLOYMENT = "SCM_DO_BUILD_DURING_DEPLOYMENT"
    }

    override fun executeSteps(processHandler: RunProcessHandler): FunctionAppBase<*, *, *> {
        OperationContext.current().setMessager(processHandlerMessenger)

        processHandler.setText("Start Function App deployment...")

        val options = requireNotNull(functionDeploymentConfiguration.state)
        val publishableProjectPath = options.publishableProjectPath
            ?: throw ExecutionException("Project is not defined")
        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { it.projectFilePath == publishableProjectPath }
            ?: throw ExecutionException("Project is not defined")

        val config = creatDotNetFunctionAppConfig(publishableProject, options)
        val createTask = CreateOrUpdateDotNetFunctionAppTask(config)
        val deployTarget = createTask.execute()

        if (deployTarget is AzResource.Draft<*, *>) {
            deployTarget.reset()
        }
        functionDeploymentConfiguration.state?.apply {
            appSettings = deployTarget.appSettings ?: mutableMapOf()
        }

        val artifactDirectory = ArtifactService.getInstance(project)
            .prepareArtifact(
                publishableProject,
                requireNotNull(options.projectConfiguration),
                requireNotNull(options.projectPlatform),
                processHandler,
                false
            )

        val deployTask = DeployDotNetFunctionAppTask(deployTarget, artifactDirectory)
        deployTask.execute()

        return deployTarget
    }

    private fun creatDotNetFunctionAppConfig(
        publishableProject: PublishableProjectModel,
        options: FunctionDeploymentConfigurationOptions
    ) = DotNetFunctionAppConfig().apply {
        subscriptionId(options.subscriptionId)
        resourceGroup(options.resourceGroupName)
        region(Region.fromName(requireNotNull(options.region)))
        servicePlanName(options.appServicePlanName)
        servicePlanResourceGroup(options.appServicePlanResourceGroupName)
        val pricingTier = PricingTier(options.pricingTier, options.pricingSize)
        pricingTier(pricingTier)
        appName(options.functionAppName)
        deploymentSlotName(options.slotName)
        val configurationSource = when (options.slotConfigurationSource) {
            "Do not clone settings" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_NEW
            "parent" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_PARENT
            null -> null
            else -> options.slotConfigurationSource
        }
        deploymentSlotConfigurationSource(configurationSource)
        storageAccountName(options.storageAccountName)
        storageAccountResourceGroup(options.storageAccountResourceGroup)
        val os = OperatingSystem.fromString(options.operatingSystem)
        runtime = createRuntimeConfig(os)
        dotnetRuntime = createDotNetRuntimeConfig(publishableProject, os)
        if (pricingTier == PricingTier.CONSUMPTION && os == OperatingSystem.LINUX) {
            options.appSettings[SCM_DO_BUILD_DURING_DEPLOYMENT] = "false"
        }
        appSettings(options.appSettings)
    }

    private fun createRuntimeConfig(os: OperatingSystem) =
        RuntimeConfig().apply {
            this.os = os
        }

    private fun createDotNetRuntimeConfig(publishableProject: PublishableProjectModel, os: OperatingSystem) =
        DotNetRuntimeConfig().apply {
            os(os)
            isDocker = false
            functionStack = runBlockingCancellable {
                publishableProject.getFunctionStack(project, os)
            }
        }

    override fun onSuccess(result: FunctionAppBase<*, *, *>, processHandler: RunProcessHandler) {
        updateConfigurationDataModel(result)
        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: FunctionAppBase<*, *, *>) {
        functionDeploymentConfiguration.state?.apply {
            if (app is FunctionAppDeploymentSlot) {
                slotName = app.name
                slotConfigurationSource = null
            }

            appSettings = app.appSettings ?: mutableMapOf()
        }
    }
}