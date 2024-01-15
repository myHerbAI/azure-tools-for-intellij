/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.CreateOrUpdateDotNetFunctionAppTask
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.ArtifactService
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.intellij.legacy.getFunctionStack
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.*
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask
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

        val publishableProjectPath = functionDeploymentConfiguration.publishableProjectPath
            ?: throw RuntimeException("Project is not defined")
        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { it.projectFilePath == publishableProjectPath }
            ?: throw RuntimeException("Project is not defined")

        val config = creatDotNetFunctionAppConfig(publishableProject)
        val createTask = CreateOrUpdateDotNetFunctionAppTask(config)
        val deployTarget = createTask.execute()

        val artifactDirectory = ArtifactService.getInstance(project)
            .prepareArtifact(
                publishableProject,
                functionDeploymentConfiguration.projectConfiguration,
                functionDeploymentConfiguration.projectPlatform,
                processHandler,
                false
            )

        val deployTask = DeployFunctionAppTask(deployTarget, artifactDirectory, FunctionDeployType.ZIP)
        deployTask.execute()

        return deployTarget
    }

    private fun creatDotNetFunctionAppConfig(publishableProject: PublishableProjectModel) =
        DotNetFunctionAppConfig().apply {
            subscriptionId(functionDeploymentConfiguration.subscriptionId)
            resourceGroup(functionDeploymentConfiguration.resourceGroup)
            region(functionDeploymentConfiguration.region)
            servicePlanName(functionDeploymentConfiguration.appServicePlanName)
            servicePlanResourceGroup(functionDeploymentConfiguration.appServicePlanResourceGroupName)
            pricingTier(functionDeploymentConfiguration.pricingTier)
            appName(functionDeploymentConfiguration.functionAppName)
            storageAccountName(functionDeploymentConfiguration.storageAccountName)
            storageAccountResourceGroup(functionDeploymentConfiguration.storageAccountResourceGroup)
            runtime(createRuntimeConfig())
            dotnetRuntime = createDotNetRuntimeConfig(publishableProject)
            if (functionDeploymentConfiguration.pricingTier == PricingTier.CONSUMPTION &&
                functionDeploymentConfiguration.operatingSystem == OperatingSystem.LINUX
            ) {
                functionDeploymentConfiguration.appSettings[SCM_DO_BUILD_DURING_DEPLOYMENT] = "false"
            }
            appSettings(functionDeploymentConfiguration.appSettings)
        }

    private fun createRuntimeConfig() = RuntimeConfig().apply {
        os(functionDeploymentConfiguration.operatingSystem)
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
    }

    private fun createDotNetRuntimeConfig(publishableProject: PublishableProjectModel) = DotNetRuntimeConfig().apply {
        val operatingSystem = functionDeploymentConfiguration.operatingSystem
        os(operatingSystem)
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
        isDocker = false
        functionStack = runBlockingCancellable {
            publishableProject.getFunctionStack(project, operatingSystem)
        }
    }

    override fun onSuccess(result: FunctionAppBase<*, *, *>, processHandler: RunProcessHandler) {
        updateConfigurationDataModel(result)
        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: FunctionAppBase<*, *, *>) {
        functionDeploymentConfiguration.apply {
//            if (app is FunctionAppDeploymentSlot) {
//            } else {
//                resourceId = app.id
//            }
            appSettings = app.appSettings ?: mutableMapOf()
        }
    }
}