/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.debuggerHelper.PlatformArchitecture
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreAttachSuspendedStartInfo
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.runtime.RunningAssemblyInfo
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfo

class FunctionAttachProfileState(
    private val runningAssemblyInfo: RunningAssemblyInfo,
    environment: ExecutionEnvironment,
    targetPlatform: PlatformArchitecture,
    private val coreToolsInfo: FunctionCoreToolsInfo,
    private val consoleInitializer: (ConsoleViewImpl) -> Unit
) : DotNetCoreAttachProfileState(runningAssemblyInfo.processInfo, environment, targetPlatform) {
    override fun initializeConsole(consoleView: ConsoleViewImpl) = consoleInitializer(consoleView)
    override suspend fun createModelStartInfo(lifetime: Lifetime) =
        DotNetCoreAttachSuspendedStartInfo(
            dotNetCoreRuntimeExecutable = coreToolsInfo.coreToolsExecutable,
            processId = processInfo.pid,
            threadId = runningAssemblyInfo.threadId,
            assemblyPath = runningAssemblyInfo.executableAssemblyPath
        )
}