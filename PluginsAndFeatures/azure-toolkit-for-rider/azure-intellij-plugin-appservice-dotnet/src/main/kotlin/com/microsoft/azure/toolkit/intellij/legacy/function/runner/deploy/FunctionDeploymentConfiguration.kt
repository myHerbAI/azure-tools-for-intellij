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

    override fun getState() =
        options as? FunctionDeploymentConfigurationOptions

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        FunctionDeploymentState(project, this)

    override fun getConfigurationEditor() = FunctionDeploymentSettingsEditor(project)

    override fun checkConfiguration() {
    }
}