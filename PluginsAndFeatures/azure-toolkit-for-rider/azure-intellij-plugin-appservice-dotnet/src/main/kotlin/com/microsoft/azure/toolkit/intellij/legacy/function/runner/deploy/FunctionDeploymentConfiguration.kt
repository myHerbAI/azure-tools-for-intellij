/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase
import com.microsoft.azure.toolkit.intellij.legacy.function.saveAppSettingsToSecurityStorage
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils
import org.apache.commons.codec.digest.DigestUtils

class FunctionDeploymentConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    RiderAzureRunConfigurationBase<FunctionDeployModel>(project, factory, name) {

    private val functionDeploymentModel = DotNetFunctionDeployModel()

    var appSettingsKey: String?
        get() = functionDeploymentModel.appSettingsKey
        set(value) {
            functionDeploymentModel.appSettingsKey = value
        }
    var projectConfiguration: String
        get() = functionDeploymentModel.projectConfiguration
        set(value) {
            functionDeploymentModel.projectConfiguration = value
        }
    var projectPlatform: String
        get() = functionDeploymentModel.projectPlatform
        set(value) {
            functionDeploymentModel.projectPlatform = value
        }

    fun setAppSettings(appSettings: Map<String, String>) {
        functionDeploymentModel.functionAppConfig.appSettings = appSettings
        functionDeploymentModel.appSettingsHash = DigestUtils.md5Hex(JsonUtils.toJson(appSettings).uppercase())
        saveAppSettingsToSecurityStorage(appSettingsKey, appSettings)
    }

    fun getAppSettings() = functionDeploymentModel.functionAppConfig?.appSettings ?: emptyMap()

    fun getPublishableProject() = functionDeploymentModel.publishableProject

    fun saveProject(projectModel: PublishableProjectModel) {
        functionDeploymentModel.publishableProject = projectModel
    }

    fun getConfig(): FunctionAppConfig? = functionDeploymentModel.functionAppConfig

    fun saveConfig(config: FunctionAppConfig) {
        functionDeploymentModel.functionAppConfig = config
        saveAppSettingsToSecurityStorage(functionDeploymentModel.appSettingsKey, config.appSettings)
    }

    override fun getModel() = functionDeploymentModel

    override fun checkConfiguration() {
        checkAzurePreconditions()
        val functionAppConfig = functionDeploymentModel.functionAppConfig
        if (functionAppConfig.resourceId.isNullOrEmpty() && functionAppConfig.name.isNullOrEmpty()) throw ConfigurationException(
            "Please specify target function"
        )
        if (functionAppConfig.runtime.operatingSystem == OperatingSystem.DOCKER) throw ConfigurationException("Invalid target, docker function is currently not supported")
        if (functionAppConfig.servicePlan == null) throw ConfigurationException("Meta-data of target function app is still loading...")
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        FunctionDeploymentState(project, this)

    override fun getConfigurationEditor() = FunctionDeploymentSettingsEditor(project, this)
}