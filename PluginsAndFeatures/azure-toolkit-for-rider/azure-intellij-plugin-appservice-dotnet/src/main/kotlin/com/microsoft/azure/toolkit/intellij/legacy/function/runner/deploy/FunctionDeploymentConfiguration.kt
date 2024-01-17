/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

class FunctionDeploymentConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    LocatableConfigurationBase<FunctionDeploymentConfigurationOptions>(project, factory, name) {
//
//    private val functionPublishModel = FunctionPublishModel()
//
//    var functionAppConfig: FunctionAppConfig
//        get() = functionPublishModel.functionAppConfig
//        set(value) {
//            functionPublishModel.functionAppConfig = value
//        }
//    var resourceId: String?
//        get() = functionPublishModel.functionAppConfig.resourceId
//        set(value) {
//            functionPublishModel.functionAppConfig.resourceId = value
//        }
//    val functionAppName: String?
//        get() = functionPublishModel.functionAppConfig.name
//    val subscriptionId: String
//        get() = functionPublishModel.functionAppConfig.subscriptionId
//    val resourceGroup: String
//        get() = functionPublishModel.functionAppConfig.resourceGroup.name
//    val region: Region
//        get() = functionPublishModel.functionAppConfig.region
//    val appServicePlanName: String?
//        get() = functionPublishModel.functionAppConfig.servicePlan.name
//    val appServicePlanResourceGroupName: String?
//        get() = functionPublishModel.functionAppConfig.servicePlan.resourceGroupName
//    val pricingTier: PricingTier
//        get() = functionPublishModel.functionAppConfig.servicePlan.pricingTier
//    val operatingSystem: OperatingSystem
//        get() = functionPublishModel.functionAppConfig.runtime.operatingSystem
//    var storageAccountName: String?
//        get() = functionPublishModel.storageAccountName
//        set(value) {
//            functionPublishModel.storageAccountName = value
//        }
//    var storageAccountResourceGroup: String?
//        get() = functionPublishModel.storageAccountResourceGroup
//        set(value) {
//            functionPublishModel.storageAccountResourceGroup = value
//        }
//    var appSettings: MutableMap<String, String>
//        get() = functionPublishModel.functionAppConfig.appSettings
//        set(value) {
//            functionPublishModel.functionAppConfig.appSettings = value
//        }
//    var appSettingsKey: String?
//        get() = functionPublishModel.appSettingsKey
//        set(value) {
//            functionPublishModel.appSettingsKey = value
//        }
//    var projectConfiguration: String
//        get() = functionPublishModel.projectConfiguration
//        set(value) {
//            functionPublishModel.projectConfiguration = value
//        }
//    var projectPlatform: String
//        get() = functionPublishModel.projectPlatform
//        set(value) {
//            functionPublishModel.projectPlatform = value
//        }
//    var publishableProjectPath: String?
//        get() = functionPublishModel.publishableProjectPath
//        set(value) {
//            functionPublishModel.publishableProjectPath = value
//        }

    override fun getState() =
        options as? FunctionDeploymentConfigurationOptions

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        FunctionDeploymentState(project, this)

    override fun getConfigurationEditor() = FunctionDeploymentSettingsEditor(project)

//    override fun getModel() = functionPublishModel

    override fun checkConfiguration() {
//        checkAzurePreconditions()
//        val functionAppConfig = functionPublishModel.functionAppConfig
//        if (functionAppConfig.resourceId.isNullOrEmpty() && functionAppConfig.name.isNullOrEmpty())
//            throw RuntimeConfigurationError("Please specify target function")
//        if (functionAppConfig.runtime?.operatingSystem == OperatingSystem.DOCKER) throw RuntimeConfigurationError("Invalid target, docker function is currently not supported")
//        if (functionAppConfig.servicePlan == null) throw RuntimeConfigurationError("Meta-data of target function app is still loading...")
//        if (publishableProjectPath == null) throw RuntimeConfigurationError("Choose a project to deploy")
    }

//    override fun readExternal(element: Element) {
//        XmlSerializer.deserializeInto(getModel(), element)
//    }
//
//    override fun writeExternal(element: Element) {
//        XmlSerializer.serializeInto(getModel(), element) { accessor, _ ->
//            !accessor.name.equals("application", true)
//        }
//    }
}