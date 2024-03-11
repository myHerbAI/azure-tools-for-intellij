/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.browsers.BrowserStarter
import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.withBackgroundContext
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities
import com.jetbrains.rider.projectView.workspace.isUnloadedProject
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfigurationParameters
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.runtime.mono.MonoRuntimeType
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfo
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfoProvider
import org.jdom.Element
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FunctionRunConfigurationParameters(
    project: Project,
    exePath: String,
    programParameters: String,
    workingDirectory: String,
    envs: Map<String, String>,
    isPassParentEnvs: Boolean,
    useExternalConsole: Boolean,
    var projectFilePath: String,
    var trackProjectExePath: Boolean,
    var trackProjectArguments: Boolean,
    var trackProjectWorkingDirectory: Boolean,
    var projectKind: RunnableProjectKind,
    var projectTfm: String,
    var functionNames: String,
    var startBrowserParameters: DotNetStartBrowserParameters
) : DotNetExeConfigurationParameters(
    project,
    exePath,
    programParameters,
    workingDirectory,
    envs,
    isPassParentEnvs,
    useExternalConsole,
    false,
    null,
    "",
    null
) {
    companion object {
        private const val PROJECT_PATH = "PROJECT_PATH"
        private const val PROJECT_EXE_PATH_TRACKING = "PROJECT_EXE_PATH_TRACKING"
        private const val PROJECT_ARGUMENTS_TRACKING = "PROJECT_ARGUMENTS_TRACKING"
        private const val PROJECT_WORKING_DIRECTORY_TRACKING = "PROJECT_WORKING_DIRECTORY_TRACKING"
        private const val PROJECT_KIND = "PROJECT_KIND"
        private const val PROJECT_TFM = "PROJECT_TFM"
        private const val FUNCTION_NAMES = "FUNCTION_NAMES"

        private val LOG = logger<FunctionRunConfigurationParameters>()

        fun createDefault(project: Project) = FunctionRunConfigurationParameters(
            project,
            "",
            "",
            "",
            hashMapOf(),
            true,
            false,
            "",
            true,
            true,
            true,
            RunnableProjectKinds.None,
            "",
            "",
            DotNetStartBrowserParameters()
        )
    }

    val isUnloadedProject: Boolean
        get() = WorkspaceModel
            .getInstance(project)
            .getProjectModelEntities(Path.of(projectFilePath), project)
            .any { it.isUnloadedProject() }

    override fun toDotNetExecutable(): DotNetExecutable {
        if (tryGetRunnableProject() == null) throw CantRunException("Project is not specified")

        val provider = FunctionCoreToolsInfoProvider.getInstance()
        val coreToolsInfo = runWithModalProgressBlocking(project, "Downloading Azure Functions Core Tools...") {
            withBackgroundContext {
                provider.retrieveForProject(project, projectFilePath, true)
            }
        }
            ?: throw CantRunException("Can't run Azure Functions host. No Azure Functions Core Tools information could be determined.")

        return toDotNetExecutable(coreToolsInfo)
    }

    fun toDotNetExecutable(coreToolsInfo: FunctionCoreToolsInfo): DotNetExecutable {
        val runnableProject = tryGetRunnableProject() ?: throw CantRunException("Project is not specified")
        val projectOutput = tryGetProjectOutput(runnableProject)

        val effectiveWorkingDirectory = if (trackProjectWorkingDirectory && projectOutput != null) {
            projectOutput.workingDirectory
        } else {
            workingDirectory
        }

        val effectiveArguments =
            if (trackProjectArguments &&
                programParameters.isEmpty() &&
                projectOutput != null &&
                projectOutput.defaultArguments.isNotEmpty()
            ) {
                ParametersListUtil.join(projectOutput.defaultArguments)
            } else {
                programParameters
            }

        return DotNetExecutable(
            exePath = coreToolsInfo.coreToolsExecutable.absolutePathString(),
            projectTfm = projectOutput?.tfm,
            workingDirectory = effectiveWorkingDirectory,
            programParameterString = effectiveArguments,
            runtimeType = runtimeType,
            useMonoRuntime = runtimeType is MonoRuntimeType,
            useExternalConsole = useExternalConsole,
            environmentVariables = envs,
            isPassParentEnvs = isPassParentEnvs,
            onBeforeProcessStarted = startBrowserAction,
            assemblyToDebug = coreToolsInfo.coreToolsExecutable.absolutePathString(),
            runtimeArguments = runtimeArguments,
            executeAsIs = true
        )
    }

    private val startBrowserAction: (ExecutionEnvironment, RunProfile, ProcessHandler) -> Unit =
        { _, runProfile, processHandler ->
            if (startBrowserParameters.startAfterLaunch && runProfile is RunConfiguration) {
                val startBrowserSettings = StartBrowserSettings().apply {
                    isSelected = startBrowserParameters.startAfterLaunch
                    url = startBrowserParameters.url
                    browser = startBrowserParameters.browser
                    isStartJavaScriptDebugger = startBrowserParameters.withJavaScriptDebugger
                }
                BrowserStarter(runProfile, startBrowserSettings, processHandler).start()
            }
        }

    private fun tryGetRunnableProject(): RunnableProject? {
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrNull
        if (runnableProjects.isNullOrEmpty()) return null

        val applicableProjects = runnableProjects.filter {
            it.projectFilePath == projectFilePath && FunctionRunConfigurationType.isTypeApplicable(it.kind)
        }

        if (applicableProjects.size > 1) {
            val runnableProjectPaths = applicableProjects.joinToString("; ")
            LOG.warn("Multiple applicable runnable projects detected for .NET Project configuration ($projectFilePath): $runnableProjectPaths")
        }

        return applicableProjects.firstOrNull()
    }

    private fun tryGetProjectOutput(runnableProject: RunnableProject) =
        runnableProject.projectOutputs.singleOrNull { it.tfm?.presentableName == projectTfm }
            ?: runnableProject.projectOutputs.firstOrNull()

    override fun validate(riderDotNetActiveRuntimeHost: RiderDotNetActiveRuntimeHost) {
        if (project.solution.isLoaded.valueOrNull != true)
            throw RuntimeConfigurationError("Solution is loading, please wait for a few seconds.")

        val runnableProject = tryGetRunnableProject()
            ?: throw RuntimeConfigurationError("Project is not specified.")

        if (!runnableProject.problems.isNullOrEmpty())
            throw RuntimeConfigurationError(runnableProject.problems)

        if (!trackProjectExePath) {
            val exeFile = File(exePath)
            if (!exeFile.exists() || !exeFile.isFile) {
                val exePath = exePath.ifEmpty { "<empty>" }
                throw RuntimeConfigurationError("Invalid exe path: '$exePath'")
            }
        }

        if (!trackProjectWorkingDirectory) {
            val workingDirectoryFile = File(workingDirectory)
            if (!workingDirectoryFile.exists() || !workingDirectoryFile.isDirectory) {
                val workingDirectoryPath = workingDirectory.ifEmpty { "<empty>" }
                throw RuntimeConfigurationError("Invalid working directory: '$workingDirectoryPath'")
            }
        }

        if (runtimeType is MonoRuntimeType && riderDotNetActiveRuntimeHost.monoRuntime == null)
            throw RuntimeConfigurationError("Mono runtime not found. Please setup Mono path in settings (File | Settings | Build, Execution, Deployment | Toolset and Build)")
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        projectFilePath = JDOMExternalizerUtil.readField(element, PROJECT_PATH) ?: ""
        val trackProjectExePathString = JDOMExternalizerUtil.readField(element, PROJECT_EXE_PATH_TRACKING) ?: ""
        trackProjectExePath = trackProjectExePathString != "0"
        val trackProjectArgumentsString = JDOMExternalizerUtil.readField(element, PROJECT_ARGUMENTS_TRACKING) ?: ""
        trackProjectArguments = trackProjectArgumentsString != "0"
        val trackProjectWorkingDirectoryString =
            JDOMExternalizerUtil.readField(element, PROJECT_WORKING_DIRECTORY_TRACKING) ?: ""
        trackProjectWorkingDirectory = trackProjectWorkingDirectoryString != "0"
        projectKind = RunnableProjectKind(JDOMExternalizerUtil.readField(element, PROJECT_KIND) ?: "None")
        projectTfm = JDOMExternalizerUtil.readField(element, PROJECT_TFM) ?: ""
        functionNames = JDOMExternalizerUtil.readField(element, FUNCTION_NAMES) ?: ""
        startBrowserParameters = DotNetStartBrowserParameters.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, PROJECT_PATH, projectFilePath)
        JDOMExternalizerUtil.writeField(element, PROJECT_EXE_PATH_TRACKING, if (trackProjectExePath) "1" else "0")
        JDOMExternalizerUtil.writeField(element, PROJECT_ARGUMENTS_TRACKING, if (trackProjectArguments) "1" else "0")
        JDOMExternalizerUtil.writeField(
            element,
            PROJECT_WORKING_DIRECTORY_TRACKING,
            if (trackProjectWorkingDirectory) "1" else "0"
        )
        JDOMExternalizerUtil.writeField(element, PROJECT_KIND, projectKind.toString())
        JDOMExternalizerUtil.writeField(element, PROJECT_TFM, projectTfm)
        JDOMExternalizerUtil.writeField(element, FUNCTION_NAMES, functionNames)
        startBrowserParameters.writeExternal(element)
    }
}