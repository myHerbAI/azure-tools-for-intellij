/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.rider.run.DotNetProcessRunProfileState
import com.jetbrains.rider.run.dotNetCore.DotNetCoreDebugProfile
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.RunningAssemblyInfo
import com.jetbrains.rider.runtime.SuspendedAttachableDotNetRuntime
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntimeType
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfo
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionWorkerRuntime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.io.path.absolutePathString

class FunctionNetCoreRuntime(
    private val coreToolsInfo: FunctionCoreToolsInfo,
    private val workerRuntime: FunctionWorkerRuntime
) : DotNetRuntime(DotNetCoreRuntimeType), SuspendedAttachableDotNetRuntime {
    override fun createDebugState(
        dotNetExecutable: DotNetExecutable,
        executionEnvironment: ExecutionEnvironment
    ) = when (workerRuntime) {
        FunctionWorkerRuntime.DOTNET -> DotNetCoreDebugProfile(
            DotNetCoreRuntime(coreToolsInfo.coreToolsExecutable.absolutePathString()),
            dotNetExecutable,
            executionEnvironment,
            coreToolsInfo.coreToolsExecutable.absolutePathString()
        )

        FunctionWorkerRuntime.DOTNET_ISOLATED -> FunctionIsolatedDebugProfileState(
            dotNetExecutable,
            this,
            executionEnvironment
        )
    }

    override fun createRunState(
        dotNetExecutable: DotNetExecutable,
        executionEnvironment: ExecutionEnvironment
    ) = DotNetProcessRunProfileState(dotNetExecutable, this, executionEnvironment)

    override fun patchRunCommandLine(commandLine: GeneralCommandLine, runtimeArguments: List<String>) {
        if (commandLine.exePath.endsWith(".dll", true)) {
            val exePath = commandLine.exePath
            if (commandLine.parametersList.parametersCount > 0 &&
                commandLine.parametersList[0] != exePath
            ) {
                commandLine.parametersList.prepend(exePath)
            }
            commandLine.exePath = coreToolsInfo.coreToolsExecutable.absolutePathString()
        }
    }

    override fun createDebugAttachSuspendedState(
        runningAssemblyInfo: RunningAssemblyInfo,
        executionEnvironment: ExecutionEnvironment,
        consoleInitializer: (ConsoleViewImpl) -> Unit
    ) = FunctionAttachProfileState(
        runningAssemblyInfo,
        executionEnvironment,
        runningAssemblyInfo.platform,
        coreToolsInfo,
        consoleInitializer
    )
}