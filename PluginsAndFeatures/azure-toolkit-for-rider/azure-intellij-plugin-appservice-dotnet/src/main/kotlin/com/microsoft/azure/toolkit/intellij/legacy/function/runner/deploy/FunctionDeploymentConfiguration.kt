/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.AppServiceProjectService
import com.microsoft.azure.toolkit.intellij.legacy.utils.*
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase

class FunctionDeploymentConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    LocatableConfigurationBase<FunctionDeploymentConfigurationOptions>(project, factory, name) {

    var publishableProjectPath: String?
        get() = getState()?.publishableProjectPath
        set(value) {
            getState()?.publishableProjectPath = value
        }

    override fun suggestedName() = "Publish Function App"

    override fun getState() = options as? FunctionDeploymentConfigurationOptions

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        FunctionDeploymentState(
            project,
            AppServiceProjectService.getInstance(project).scope.childScope("FunctionDeploymentState"),
            this
        )

    override fun getConfigurationEditor() = FunctionDeploymentSettingsEditor(project)

    override fun checkConfiguration() {
        val options = getState() ?: return
        with(options) {
            isAccountSignedIn()
            if (functionAppName.isNullOrEmpty()) throw RuntimeConfigurationError("Function App name is not provided")
            if (!isValidApplicationName(functionAppName)) throw RuntimeConfigurationError(APPLICATION_VALIDATION_MESSAGE)
            if (subscriptionId.isNullOrEmpty()) throw RuntimeConfigurationError("Subscription is not provided")
            if (resourceGroupName.isNullOrEmpty()) throw RuntimeConfigurationError("Resource group is not provided")
            if (!isValidResourceGroupName(resourceGroupName)) throw RuntimeConfigurationError(RESOURCE_GROUP_VALIDATION_MESSAGE)
            if (region.isNullOrEmpty()) throw RuntimeConfigurationError("Region is not provided")
            if (appServicePlanName.isNullOrEmpty()) throw RuntimeConfigurationError("App Service plan name is not provided")
            if (!isValidApplicationName(appServicePlanName)) throw RuntimeConfigurationError("App Service plan names only allow alphanumeric characters and hyphens, cannot start or end in a hyphen, and must be less than 60 chars")
            if (appServicePlanResourceGroupName.isNullOrEmpty()) throw RuntimeConfigurationError("App Service plan resource group is not provided")
            if (!isValidResourceGroupName(appServicePlanResourceGroupName)) throw RuntimeConfigurationError(RESOURCE_GROUP_VALIDATION_MESSAGE)
            if (pricingTier.isNullOrEmpty()) throw RuntimeConfigurationError("Pricing tier is not provided")
            if (pricingSize.isNullOrEmpty()) throw RuntimeConfigurationError("Pricing size is not provided")

            if (isDeployToSlot) {
                if (slotName.isNullOrEmpty()) throw RuntimeConfigurationError("Deployment slot name is not provided")
                if (!isValidApplicationSlotName(slotName)) throw RuntimeConfigurationError(APPLICATION_SLOT_VALIDATION_MESSAGE)
            }

            if (operatingSystem.isNullOrEmpty()) throw RuntimeConfigurationError("Operating system is not provided")
            if (publishableProjectPath.isNullOrEmpty()) throw RuntimeConfigurationError("Choose a project to deploy")
            if (projectPlatform.isNullOrEmpty()) throw RuntimeConfigurationError("Choose a project platform")
            if (projectConfiguration.isNullOrEmpty()) throw RuntimeConfigurationError("Choose a project configuration")
        }
    }

    fun setFunctionApp(functionApp: FunctionAppBase<*, *, *>) {
        getState()?.apply {
            functionAppName = functionApp.name
            subscriptionId = functionApp.subscriptionId
            resourceGroupName = functionApp.resourceGroupName
            region = functionApp.region.toString()
            appServicePlanName = functionApp.appServicePlan?.name
            appServicePlanResourceGroupName = functionApp.appServicePlan?.resourceGroupName
            pricingTier = functionApp.appServicePlan?.pricingTier?.tier
            pricingSize = functionApp.appServicePlan?.pricingTier?.size
            operatingSystem = functionApp.runtime?.operatingSystem?.toString()
            isDeployToSlot = false
            slotName = null
            slotConfigurationSource = null
            storageAccountName = null
            storageAccountResourceGroup = null
            appSettings = functionApp.appSettings ?: mutableMapOf()
        }
    }
}