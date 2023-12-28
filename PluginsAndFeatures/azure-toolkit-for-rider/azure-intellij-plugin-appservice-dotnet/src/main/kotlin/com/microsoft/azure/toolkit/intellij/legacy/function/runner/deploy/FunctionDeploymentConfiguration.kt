/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.common.model.Region
import org.jdom.Element

class FunctionDeploymentConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    RiderAzureRunConfigurationBase<FunctionDeployModel>(project, factory, name) {

    private val functionPublishModel = FunctionPublishModel()

    var functionAppConfig: FunctionAppConfig
        get() = functionPublishModel.functionAppConfig
        set(value) {
            functionPublishModel.functionAppConfig = value
        }
    val resourceId: String?
        get() = functionPublishModel.functionAppConfig.resourceId
    val functionAppName: String?
        get() = functionPublishModel.functionAppConfig.name
    val subscriptionId: String
        get() = functionPublishModel.functionAppConfig.subscriptionId
    val resourceGroup: String
        get() = functionPublishModel.functionAppConfig.resourceGroup.name
    val region: Region
        get() = functionPublishModel.functionAppConfig.region
    val appServicePlanName: String?
        get() = functionPublishModel.functionAppConfig.servicePlan.name
    val appServicePlanResourceGroupName: String?
        get() = functionPublishModel.functionAppConfig.servicePlan.resourceGroupName
    val pricingTier: PricingTier
        get() = functionPublishModel.functionAppConfig.servicePlan.pricingTier
    val operatingSystem: OperatingSystem
        get() = functionPublishModel.functionAppConfig.runtime.operatingSystem
    var appSettings: MutableMap<String, String>
        get() = functionPublishModel.functionAppConfig.appSettings
        set(value) {
            functionPublishModel.functionAppConfig.appSettings = value
        }
    var appSettingsKey: String?
        get() = functionPublishModel.appSettingsKey
        set(value) {
            functionPublishModel.appSettingsKey = value
        }
    var projectConfiguration: String
        get() = functionPublishModel.projectConfiguration
        set(value) {
            functionPublishModel.projectConfiguration = value
        }
    var projectPlatform: String
        get() = functionPublishModel.projectPlatform
        set(value) {
            functionPublishModel.projectPlatform = value
        }
    var publishableProjectPath: String?
        get() = functionPublishModel.publishableProjectPath
        set(value) {
            functionPublishModel.publishableProjectPath = value
        }

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        FunctionDeploymentState(project, this)

    override fun getConfigurationEditor() = FunctionDeploymentSettingsEditor(project, this)

    override fun getModel() = functionPublishModel

    override fun checkConfiguration() {
        checkAzurePreconditions()
        val functionAppConfig = functionPublishModel.functionAppConfig
        if (functionAppConfig.resourceId.isNullOrEmpty() && functionAppConfig.name.isNullOrEmpty()) throw ConfigurationException(
            "Please specify target function"
        )
        if (functionAppConfig.runtime?.operatingSystem == OperatingSystem.DOCKER) throw ConfigurationException("Invalid target, docker function is currently not supported")
        if (functionAppConfig.servicePlan == null) throw ConfigurationException("Meta-data of target function app is still loading...")
    }

    override fun readExternal(element: Element) {
        XmlSerializer.deserializeInto(getModel(), element)
    }

    override fun writeExternal(element: Element) {
        XmlSerializer.serializeInto(getModel(), element) { accessor, _ ->
            !accessor.name.equals("application", true)
        }
    }
}