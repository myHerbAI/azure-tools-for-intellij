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
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.Constants
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp


class WebAppConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
        RiderAzureRunConfigurationBase<DotNetWebAppSettingModel>(project, factory, name), IWebAppRunConfiguration,
        IConnectionAware {
    companion object {
        private const val SLOT_NAME_REGEX_PATTERN = "[a-zA-Z0-9-]{1,60}"
    }

    val webAppSettingModel = DotNetWebAppSettingModel()

    private val slotNameRegex = Regex(SLOT_NAME_REGEX_PATTERN)

    var webAppId: String?
        get() = webAppSettingModel.webAppId
        set(value) {
            webAppSettingModel.webAppId = value
        }
    var webAppName: String
        get() = webAppSettingModel.webAppName
        set(value) {
            webAppSettingModel.webAppName = value
        }
    var subscriptionId: String
        get() = webAppSettingModel.subscriptionId
        set(value) {
            webAppSettingModel.subscriptionId = value
        }
    var region: String
        get() = webAppSettingModel.region
        set(value) {
            webAppSettingModel.region = value
        }
    var resourceGroup: String
        get() = webAppSettingModel.resourceGroup
        set(value) {
            webAppSettingModel.resourceGroup = value
        }
    var pricing: String
        get() = webAppSettingModel.pricing
        set(value) {
            webAppSettingModel.pricing = value
        }
    val operatingSystem: OperatingSystem?
        get() = OperatingSystem.fromString(webAppSettingModel.operatingSystem)
    var appServicePlanName: String?
        get() = webAppSettingModel.appServicePlanName
        set(value) {
            webAppSettingModel.appServicePlanName = value
        }
    var appServicePlanResourceGroupName: String?
        get() = webAppSettingModel.appServicePlanResourceGroupName
        set(value) {
            webAppSettingModel.appServicePlanResourceGroupName = value
        }
    var isCreatingAppServicePlan: Boolean
        get() = webAppSettingModel.isCreatingAppServicePlan
        set(value) {
            webAppSettingModel.isCreatingAppServicePlan = value
        }
    var isCreatingNew: Boolean
        get() = webAppSettingModel.isCreatingNew
        set(value) {
            webAppSettingModel.isCreatingNew = value
        }
    var isOpenBrowserAfterDeployment: Boolean
        get() = webAppSettingModel.isOpenBrowserAfterDeployment
        set(value) {
            webAppSettingModel.isOpenBrowserAfterDeployment = value
        }
    var isSlotPanelVisible: Boolean
        get() = webAppSettingModel.slotPanelVisible
        set(value) {
            webAppSettingModel.slotPanelVisible = value
        }
    var isDeployToSlot: Boolean
        get() = webAppSettingModel.isDeployToSlot
        set(value) {
            webAppSettingModel.isDeployToSlot = value
        }
    var newSlotConfigurationSource: String
        get() = webAppSettingModel.newSlotConfigurationSource
        set(value) {
            webAppSettingModel.newSlotConfigurationSource = value
        }
    var slotName: String?
        get() = webAppSettingModel.slotName
        set(value) {
            webAppSettingModel.slotName = value
        }
    var newSlotName: String
        get() = webAppSettingModel.newSlotName
        set(value) {
            webAppSettingModel.newSlotName = value
        }
    var appSettingsKey: String
        get() = webAppSettingModel.appSettingsKey
        set(value) {
            webAppSettingModel.appSettingsKey = value
        }
    var projectConfiguration: String
        get() = webAppSettingModel.projectConfiguration
        set(value) {
            webAppSettingModel.projectConfiguration = value
        }
    var projectPlatform: String
        get() = webAppSettingModel.projectPlatform
        set(value) {
            webAppSettingModel.projectPlatform = value
        }
    var appSettingsToRemove: Set<String>
        get() = webAppSettingModel.appSettingsToRemove
        set(value) {
            webAppSettingModel.appSettingsToRemove = value
        }
    val getRuntime: Runtime?
        get() = webAppSettingModel.runtime

    fun saveRuntime(runtime: Runtime?, projectModel: PublishableProjectModel?) {
        if (runtime == null || projectModel == null) webAppSettingModel.saveRuntime(null)
        else webAppSettingModel.saveRuntime(projectModel.toRuntime(project, runtime.operatingSystem))
    }

    fun getProjectId(): Int? = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { p -> p.projectFilePath == webAppSettingModel.projectPath }
            ?.projectModelId

    fun saveProject(projectModel: PublishableProjectModel) {
        webAppSettingModel.projectPath = projectModel.projectFilePath
    }

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) =
            WebAppRunState(project, this)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
            WebAppSettingEditor(project, this)

    override fun getModel() = webAppSettingModel

    override fun setApplicationSettings(env: Map<String, String>) {
        webAppSettingModel.appSettings = env
    }

    override fun getApplicationSettings(): Map<String, String> = webAppSettingModel.appSettings

    override fun getModule(): Module? = null

    override fun validate() {
        with(webAppSettingModel) {
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
            if (projectPath.isEmpty()) throw ConfigurationException("Choose a project to deploy")
        }
    }

    fun setWebApp(webApp: WebApp) {
        webAppId = webApp.id
        webAppName = webApp.name
        subscriptionId = webApp.subscriptionId
        resourceGroup = webApp.resourceGroupName
        webApp.appServicePlan?.let {
            appServicePlanName = it.name
            appServicePlanResourceGroupName = it.resourceGroupName
        }
        webApp.region?.let { region = it.name }
        webApp.appSettings?.let { applicationSettings = it }
    }
}