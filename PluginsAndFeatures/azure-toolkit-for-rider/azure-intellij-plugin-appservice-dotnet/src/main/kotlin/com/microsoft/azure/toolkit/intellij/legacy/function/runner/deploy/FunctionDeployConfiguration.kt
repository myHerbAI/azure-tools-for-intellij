package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase

class FunctionDeployConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?):
        RiderAzureRunConfigurationBase<FunctionDeployModel>(project, factory, name) {
    override fun getModel(): FunctionDeployModel {
        TODO("Not yet implemented")
    }

    override fun validate() {
        TODO("Not yet implemented")
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        TODO("Not yet implemented")
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        TODO("Not yet implemented")
    }
}