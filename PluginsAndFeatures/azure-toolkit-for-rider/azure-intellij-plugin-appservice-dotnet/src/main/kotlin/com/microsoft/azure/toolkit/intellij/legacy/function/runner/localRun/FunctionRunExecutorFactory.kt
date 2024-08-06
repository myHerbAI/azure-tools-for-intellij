/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DialogTitleCapitalization")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.browsers.BrowserStarter
import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.AsyncExecutorFactory
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.msNet.MsNetRuntime
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfo
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfoProvider
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.*
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettingsService
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionWorkerRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class FunctionRunExecutorFactory(
    private val project: Project,
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
        val coreToolsInfoResult = FunctionCoreToolsInfoProvider
            .getInstance()
            .retrieveForProject(project, parameters.projectFilePath, true)
            ?: throw CantRunException("Can't run Azure Functions host. No Azure Functions Core Tools information could be determined")
        val (azureFunctionsVersion, coreToolsInfo) = coreToolsInfoResult

        val functionLocalSettings = withContext(Dispatchers.Default) {
            FunctionLocalSettingsService
                .getInstance(project)
                .getFunctionLocalSettings(Path(parameters.projectFilePath).parent)
        }

        val dotNetExecutable = getDotNetExecutable(coreToolsInfo, functionLocalSettings)
            ?: throw CantRunException("Can't run Azure Functions host. Unable to create .NET executable")

        LOG.debug("Patching host.json file to reflect run configuration parameters")
        HostJsonPatcher
            .getInstance()
            .tryPatchHostJsonFile(dotNetExecutable.workingDirectory, parameters.functionNames)

        val workerRuntime = functionLocalSettings?.values?.workerRuntime ?: FunctionWorkerRuntime.DOTNET_ISOLATED
        LOG.debug { "Worker runtime: $workerRuntime" }

        val runtimeToExecute = if (azureFunctionsVersion.equals("v1", ignoreCase = true)) {
            MsNetRuntime()
        } else {
            FunctionNetCoreRuntime(coreToolsInfo, workerRuntime, lifetime)
        }

        LOG.debug { "Configuration will be executed on ${runtimeToExecute.javaClass.name}" }
        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> runtimeToExecute.createRunState(dotNetExecutable, environment)
            DefaultDebugExecutor.EXECUTOR_ID -> runtimeToExecute.createDebugState(dotNetExecutable, environment)
            else -> throw CantRunException("Unsupported executor $executorId")
        }
    }

    private fun getDotNetExecutable(
        coreToolsInfo: FunctionCoreToolsInfo,
        functionLocalSettings: FunctionLocalSettings?
    ): DotNetExecutable? {
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrNull
        if (runnableProjects.isNullOrEmpty()) return null

        val runnableProject = runnableProjects.singleOrNull {
            it.projectFilePath == parameters.projectFilePath && it.kind == AzureRunnableProjectKinds.AzureFunctions
        }
        if (runnableProject == null) {
            LOG.warn("Unable to get the runnable project with path ${parameters.projectFilePath}")
            return null
        }

        val projectOutput = runnableProject
            .projectOutputs
            .singleOrNull { it.tfm?.presentableName == parameters.projectTfm }
        if (projectOutput == null) {
            LOG.warn("Unable to get the project output for ${parameters.projectTfm}")
            return null
        }

        val launchProfile = FunctionLaunchProfilesService
            .getInstance(project)
            .getLaunchProfileByName(runnableProject, parameters.profileName)
        if (launchProfile == null) {
            LOG.warn("Unable to get the launch profile with name ${parameters.profileName}")
            return null
        }

        val effectiveArguments =
            if (parameters.trackArguments) getArguments(launchProfile.content, projectOutput)
            else parameters.arguments

        val effectiveWorkingDirectory =
            if (parameters.trackWorkingDirectory) getWorkingDirectory(launchProfile.content, projectOutput)
            else parameters.workingDirectory

        val effectiveEnvs =
            if (parameters.trackEnvs) getEnvironmentVariables(launchProfile.content)
            else parameters.envs

        val effectiveUrl =
            if (parameters.trackUrl) getApplicationUrl(launchProfile.content, projectOutput, functionLocalSettings)
            else parameters.startBrowserParameters.url

        val coreToolsExecutablePath = coreToolsInfo.coreToolsExecutable.absolutePathString()

        return DotNetExecutable(
            coreToolsExecutablePath,
            projectOutput.tfm,
            effectiveWorkingDirectory,
            effectiveArguments,
            false,
            parameters.useExternalConsole,
            effectiveEnvs,
            true,
            getStartBrowserAction(effectiveUrl, parameters.startBrowserParameters),
            coreToolsExecutablePath,
            "",
            true
        )
    }

    private fun getStartBrowserAction(
        browserUrl: String,
        params: DotNetStartBrowserParameters
    ): (ExecutionEnvironment, RunProfile, ProcessHandler) -> Unit =
        { _, runProfile, processHandler ->
            if (params.startAfterLaunch && runProfile is RunConfiguration) {
                val startBrowserSettings = StartBrowserSettings().apply {
                    isSelected = params.startAfterLaunch
                    url = browserUrl
                    browser = params.browser
                    isStartJavaScriptDebugger = params.withJavaScriptDebugger
                }
                BrowserStarter(runProfile, startBrowserSettings, processHandler).start()
            }
        }
}