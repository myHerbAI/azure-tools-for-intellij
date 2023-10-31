/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase

class FunctionDeploymentConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    RiderAzureRunConfigurationBase<FunctionDeployModel>(project, factory, name) {

    private val functionDeploymentModel = FunctionDeployModel()

    var appSettingsKey: String?
        get() = functionDeploymentModel.appSettingsKey
        set(value) {
            functionDeploymentModel.appSettingsKey = value
        }

    override fun getModel() = functionDeploymentModel

    override fun validate() {
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        FunctionDeploymentState(project, this)

    override fun getConfigurationEditor() = FunctionDeploymentSettingsEditor(project, this)
}