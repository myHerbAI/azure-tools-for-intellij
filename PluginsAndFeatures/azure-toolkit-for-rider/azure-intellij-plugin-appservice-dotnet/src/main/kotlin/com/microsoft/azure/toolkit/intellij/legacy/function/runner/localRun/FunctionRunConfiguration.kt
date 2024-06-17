/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.rider.debugger.IRiderDebuggable
import com.jetbrains.rider.run.configurations.IAutoSelectableRunConfiguration
import com.jetbrains.rider.run.configurations.RiderAsyncRunConfiguration
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import org.jdom.Element

class FunctionRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String?,
    val parameters: FunctionRunConfigurationParameters
) : RiderAsyncRunConfiguration(
    name ?: "Azure Functions",
    project,
    factory,
    { FunctionRunSettingsEditor(it) },
    FunctionRunExecutorFactory(parameters)
), IRiderDebuggable, IAutoSelectableRunConfiguration {

    private val riderDotNetActiveRuntimeHost = RiderDotNetActiveRuntimeHost.getInstance(project)

    override fun checkConfiguration() {
        super.checkConfiguration()
        parameters.validate(riderDotNetActiveRuntimeHost)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        parameters.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        parameters.writeExternal(element)
    }

    override fun getAutoSelectPriority() = 10
}