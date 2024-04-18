/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.rd.util.withBackgroundContext
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.run.configurations.AsyncExecutorFactory
import com.jetbrains.rider.runtime.msNet.MsNetRuntime
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfoProvider
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsMsBuildService
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings.FunctionLocalSettingsUtil
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings.FunctionWorkerRuntime
import java.io.File

class FunctionRunExecutorFactory(
    private val parameters: FunctionRunConfigurationParameters
) : AsyncExecutorFactory {
    companion object {
        private val LOG = logger<FunctionRunExecutorFactory>()
    }

    override suspend fun create(
        executorId: String,
        environment: ExecutionEnvironment,
        lifetime: Lifetime
    ): RunProfileState {
        val projectKind = parameters.projectKind
        LOG.debug("Project kind is $projectKind")

        val azureFunctionsVersion = FunctionCoreToolsMsBuildService.getInstance()
            .requestAzureFunctionsVersion(parameters.project, parameters.projectFilePath)
            ?: throw CantRunException("Can't run Azure Functions host. No Azure Functions Core Tools information could be determined.")
        LOG.debug("Azure Functions version: $azureFunctionsVersion")

        val coreToolsInfo = withBackgroundContext {
            FunctionCoreToolsInfoProvider.getInstance()
                .retrieveForVersion(azureFunctionsVersion, true)
        }
            ?: throw CantRunException("Can't run Azure Functions host. No Azure Functions Core Tools information could be determined.")
        LOG.debug("Core tools executable: ${coreToolsInfo.coreToolsExecutable}")

        LOG.debug("Patching host.json file to reflect run configuration parameters")
        HostJsonPatcher.getInstance()
            .tryPatchHostJsonFile(parameters.workingDirectory, parameters.functionNames)

        LOG.debug("Determine worker runtime from local.settings.json")
        val functionLocalSettings = withBackgroundContext {
            FunctionLocalSettingsUtil.readFunctionLocalSettings(File(parameters.projectFilePath).parent)
        }
        val workerRuntime = functionLocalSettings?.values?.workerRuntime ?: FunctionWorkerRuntime.DOTNET
        LOG.debug("Worker runtime: $workerRuntime")

        val dotNetExecutable = parameters.toDotNetExecutable(coreToolsInfo)

        val runtimeToExecute = if (azureFunctionsVersion.equals("v1", ignoreCase = true)) {
            MsNetRuntime()
        } else {
            FunctionNetCoreRuntime(coreToolsInfo, workerRuntime)
        }

        LOG.debug("Configuration will be executed on ${runtimeToExecute.javaClass.name}")
        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> runtimeToExecute.createRunState(dotNetExecutable, environment)
            DefaultDebugExecutor.EXECUTOR_ID -> runtimeToExecute.createDebugState(dotNetExecutable, environment)
            else -> throw CantRunException("Unsupported executor $executorId")
        }
    }
}