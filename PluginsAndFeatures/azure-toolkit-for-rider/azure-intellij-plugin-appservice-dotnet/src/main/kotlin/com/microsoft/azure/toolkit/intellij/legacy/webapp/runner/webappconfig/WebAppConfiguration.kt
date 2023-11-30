/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.Constants
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp


class WebAppConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    RiderAzureRunConfigurationBase<WebAppPublishModel>(project, factory, name), IWebAppRunConfiguration,
    IConnectionAware {
    companion object {
        private const val SLOT_NAME_REGEX_PATTERN = "[a-zA-Z0-9-]{1,60}"
    }

    private val webAppPublishModel = WebAppPublishModel()

    private val slotNameRegex = Regex(SLOT_NAME_REGEX_PATTERN)

    var webAppId: String?
        get() = webAppPublishModel.webAppId
        set(value) {
            webAppPublishModel.webAppId = value
        }
    var webAppName: String
        get() = webAppPublishModel.webAppName
        set(value) {
            webAppPublishModel.webAppName = value
        }
    var subscriptionId: String
        get() = webAppPublishModel.subscriptionId
        set(value) {
            webAppPublishModel.subscriptionId = value
        }
    var region: String
        get() = webAppPublishModel.region
        set(value) {
            webAppPublishModel.region = value
        }
    var resourceGroup: String
        get() = webAppPublishModel.resourceGroup
        set(value) {
            webAppPublishModel.resourceGroup = value
        }
    var pricing: String
        get() = webAppPublishModel.pricing
        set(value) {
            webAppPublishModel.pricing = value
        }
    var appServicePlanName: String?
        get() = webAppPublishModel.appServicePlanName
        set(value) {
            webAppPublishModel.appServicePlanName = value
        }
    var appServicePlanResourceGroupName: String?
        get() = webAppPublishModel.appServicePlanResourceGroupName
        set(value) {
            webAppPublishModel.appServicePlanResourceGroupName = value
        }
    var isCreatingAppServicePlan: Boolean
        get() = webAppPublishModel.isCreatingAppServicePlan
        set(value) {
            webAppPublishModel.isCreatingAppServicePlan = value
        }
    var isCreatingNew: Boolean
        get() = webAppPublishModel.isCreatingNew
        set(value) {
            webAppPublishModel.isCreatingNew = value
        }
    var isOpenBrowserAfterDeployment: Boolean
        get() = webAppPublishModel.isOpenBrowserAfterDeployment
        set(value) {
            webAppPublishModel.isOpenBrowserAfterDeployment = value
        }
    var isSlotPanelVisible: Boolean
        get() = webAppPublishModel.slotPanelVisible
        set(value) {
            webAppPublishModel.slotPanelVisible = value
        }
    var isDeployToSlot: Boolean
        get() = webAppPublishModel.isDeployToSlot
        set(value) {
            webAppPublishModel.isDeployToSlot = value
        }
    var newSlotConfigurationSource: String
        get() = webAppPublishModel.newSlotConfigurationSource
        set(value) {
            webAppPublishModel.newSlotConfigurationSource = value
        }
    var slotName: String?
        get() = webAppPublishModel.slotName
        set(value) {
            webAppPublishModel.slotName = value
        }
    var newSlotName: String
        get() = webAppPublishModel.newSlotName
        set(value) {
            webAppPublishModel.newSlotName = value
        }
    var appSettingsKey: String
        get() = webAppPublishModel.appSettingsKey
        set(value) {
            webAppPublishModel.appSettingsKey = value
        }
    var appSettingsToRemove: Set<String>
        get() = webAppPublishModel.appSettingsToRemove
        set(value) {
            webAppPublishModel.appSettingsToRemove = value
        }
    var runtime: Runtime?
        get() = webAppPublishModel.runtime
        set(value) {
            webAppPublishModel.saveRuntime(value)
        }
    var projectConfiguration: String
        get() = webAppPublishModel.projectConfiguration
        set(value) {
            webAppPublishModel.projectConfiguration = value
        }
    var projectPlatform: String
        get() = webAppPublishModel.projectPlatform
        set(value) {
            webAppPublishModel.projectPlatform = value
        }
    var publishableProjectPath: String?
        get() = webAppPublishModel.publishableProjectPath
        set(value) {
            webAppPublishModel.publishableProjectPath = value
        }

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) =
        WebAppRunState(project, this)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        WebAppSettingEditor(project, this)

    override fun getModel() = webAppPublishModel

    override fun setApplicationSettings(env: Map<String, String>) {
        webAppPublishModel.appSettings = env
    }

    override fun getApplicationSettings(): Map<String, String> = webAppPublishModel.appSettings

    override fun getModule(): Module? = null

    override fun validate() {
        with(webAppPublishModel) {
            if (isCreatingNew) {
                if (webAppName.isNullOrEmpty()) throw ConfigurationException("Web App name not provided")
                if (subscriptionId.isNullOrEmpty()) throw ConfigurationException("Subscription not provided")
                if (resourceGroup.isNullOrEmpty()) throw ConfigurationException("Resource Group not provided")
                if (isCreatingAppServicePlan) {
                    if (region.isNullOrEmpty()) throw ConfigurationException("Location not provided")
                    if (pricing.isNullOrEmpty()) throw ConfigurationException("Pricing Tier not provided")
                }
                if (appServicePlanName.isNullOrEmpty()) throw ConfigurationException("App Service Plan not provided")
            } else {
                if (webAppId.isNullOrEmpty()) throw ConfigurationException("Choose a web app to deploy")
                if (appServicePlanName.isNullOrEmpty()) throw ConfigurationException("Meta-data of target webapp is still loading...")
                if (isDeployToSlot) {
                    if (slotName == Constants.CREATE_NEW_SLOT) {
                        if (newSlotName.isNullOrEmpty()) throw ConfigurationException("The deployment slot name is not provided")
                        if (!slotNameRegex.matches(newSlotName)) throw ConfigurationException("The slot name is invalid")
                    } else if (slotName.isNullOrEmpty()) throw ConfigurationException("The deployment slot name is not provided")
                }
            }

            if (OperatingSystem.fromString(operatingSystem) == OperatingSystem.DOCKER) throw ConfigurationException("Invalid target, please change to use `Deploy Image to Web App` for docker web app")
            if (publishableProjectPath == null) throw ConfigurationException("Choose a project to deploy")
        }
    }

    fun setWebApp(webApp: WebApp) {
        webAppId = webApp.id
        webAppName = webApp.name
        subscriptionId = webApp.subscriptionId
        resourceGroup = webApp.resourceGroupName
        webApp.runtime?.let { runtime = it }
        webApp.appServicePlan?.let {
            appServicePlanName = it.name
            appServicePlanResourceGroupName = it.resourceGroupName
        }
        webApp.region?.let { region = it.name }
        webApp.appSettings?.let { applicationSettings = it }
    }
}