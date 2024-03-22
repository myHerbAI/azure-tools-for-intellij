/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.ide.model.EnvironmentVariable
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.model.*
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.runtimeSelection.RuntimeSelector
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettings
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfigurationViewModel
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings.FunctionLocalSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings.FunctionLocalSettingsUtil
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings.FunctionWorkerRuntime
import org.apache.http.client.utils.URIBuilder
import java.io.File

class FunctionRunConfigurationViewModel(
    private val lifetime: Lifetime,
    project: Project,
    private val runnableProjectsModel: RunnableProjectsModel,
    val projectSelector: ProjectSelector,
    val tfmSelector: StringSelector,
    programParametersEditor: ProgramParametersEditor,
    workingDirectorySelector: PathSelector,
    val functionNamesEditor: TextEditor,
    environmentVariablesEditor: EnvironmentVariablesEditor,
    useExternalConsoleEditor: FlagEditor,
    separator: ViewSeparator,
    val urlEditor: TextEditor,
    val dotNetBrowserSettingsEditor: BrowserSettingsEditor
) : DotNetExeConfigurationViewModel(
    lifetime,
    project,
    PathSelector("", "Exe_path", null, lifetime),
    programParametersEditor,
    workingDirectorySelector,
    environmentVariablesEditor,
    RuntimeSelector("", "Runtime", project, lifetime),
    ProgramParametersEditor("", "Runtime_arguments", lifetime),
    false,
    useExternalConsoleEditor
) {
    companion object {
        private val portRegex = Regex("(--port|-p) (\\d+)", RegexOption.IGNORE_CASE)
    }

    override val controls: List<ControlBase> = listOf(
        projectSelector,
        tfmSelector,
        programParametersEditor,
        workingDirectorySelector,
        functionNamesEditor,
        environmentVariablesEditor,
        useExternalConsoleEditor,
        separator,
        urlEditor,
        dotNetBrowserSettingsEditor
    )

    private var isLoaded = false

    var trackProjectExePath = true
    var trackProjectArguments = true
    var trackProjectWorkingDirectory = true

    private var functionLocalSettings: FunctionLocalSettings? = null

    init {
        disable()

        projectSelector.bindTo(
            runnableProjectsModel = runnableProjectsModel,
            lifetime = lifetime,
            projectFilter = { runnableProject: RunnableProject ->
                FunctionRunConfigurationType.isTypeApplicable(runnableProject.kind)
            },
            onLoad = ::enable,
            onSelect = ::handleProjectSelection
        )

        tfmSelector.string.advise(lifetime) { handleChangeTfmSelection() }
        exePathSelector.path.advise(lifetime) { recalculateTrackProjectOutput() }
        programParametersEditor.parametersString.advise(lifetime) { recalculateTrackProjectOutput() }
        workingDirectorySelector.path.advise(lifetime) { recalculateTrackProjectOutput() }

        disableBrowserUrl()
    }

    private fun disableBrowserUrl() {
        urlEditor.isEnabled.set(false)
    }

    private fun handleProjectSelection(runnableProject: RunnableProject) {
        if (!isLoaded) return

        reloadTfmSelector(runnableProject)
        readLocalSettingsForProject(runnableProject)
        composeUrlString(getPortValue())

        val startBrowserUrl =
            runnableProject.customAttributes.singleOrNull { it.key == Key.StartBrowserUrl }?.value ?: ""
        val launchBrowser =
            runnableProject.customAttributes.singleOrNull { it.key == Key.LaunchBrowser }?.value?.toBoolean() ?: false
        if (startBrowserUrl.isNotEmpty()) {
            urlEditor.defaultValue.set(startBrowserUrl)
            urlEditor.text.set(startBrowserUrl)
            dotNetBrowserSettingsEditor.settings.set(
                BrowserSettings(
                    startAfterLaunch = launchBrowser,
                    withJavaScriptDebugger = dotNetBrowserSettingsEditor.settings.value.withJavaScriptDebugger,
                    myBrowser = dotNetBrowserSettingsEditor.settings.value.myBrowser
                )
            )
        }

        environmentVariablesEditor.envs.set(runnableProject.environmentVariables.associate { it.key to it.value })
    }

    private fun handleChangeTfmSelection() {
        projectSelector.project.valueOrNull?.projectOutputs
            ?.singleOrNull { it.tfm?.presentableName == tfmSelector.string.valueOrNull }
            ?.let { projectOutput ->
                val shouldChangeExePath = trackProjectExePath
                val shouldChangeWorkingDirectory = trackProjectWorkingDirectory
                if (shouldChangeExePath) {
                    exePathSelector.path.set(projectOutput.exePath)
                }
                if (shouldChangeWorkingDirectory) {
                    workingDirectorySelector.path.set(projectOutput.workingDirectory)
                }
                exePathSelector.defaultValue.set(projectOutput.exePath)

                if (programParametersEditor.parametersString.value.isEmpty()) {
                    if (projectOutput.defaultArguments.isNotEmpty()) {
                        programParametersEditor.parametersString.set(ParametersListUtil.join(projectOutput.defaultArguments))
                        programParametersEditor.defaultValue.set(ParametersListUtil.join(projectOutput.defaultArguments))
                    } else {
                        programParametersEditor.parametersString.set("")
                        programParametersEditor.defaultValue.set("")
                    }
                }
                workingDirectorySelector.defaultValue.set(projectOutput.workingDirectory)
            }
    }

    private fun recalculateTrackProjectOutput() {
        val selectedProject = projectSelector.project.valueOrNull ?: return
        val selectedTfm = tfmSelector.string.valueOrNull ?: return

        val programParameters = programParametersEditor.parametersString.value

        selectedProject.projectOutputs.singleOrNull { it.tfm?.presentableName == selectedTfm }?.let { projectOutput ->
            trackProjectExePath = exePathSelector.path.value == projectOutput.exePath

            val defaultArguments = projectOutput.defaultArguments
            trackProjectArguments = (defaultArguments.isEmpty() ||
                    ParametersListUtil.parse(programParameters)
                        .let { params -> params.containsAll(defaultArguments) && params.size == defaultArguments.size })

            trackProjectWorkingDirectory = workingDirectorySelector.path.value == projectOutput.workingDirectory
        }

        composeUrlString(getPortValue())
    }

    private fun getPortValue(): Int = getPortFromCommandArguments() ?: getPortFromLocalSettingsFile() ?: 7071

    private fun getPortFromCommandArguments(): Int? {
        val programParameters = programParametersEditor.parametersString.value
        val parametersPortMatch = portRegex.find(programParameters)
        return parametersPortMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
    }

    private fun getPortFromLocalSettingsFile(): Int? = functionLocalSettings?.host?.localHttpPort

    private fun composeUrlString(port: Int?) {
        if (port == null) return

        val currentUrl = urlEditor.text.value
        val originalUrl = currentUrl.ifEmpty { "http://localhost" }

        val updatedUrl = URIBuilder(originalUrl).setPort(port).build().toString()

        urlEditor.text.set(updatedUrl)
        urlEditor.defaultValue.set(updatedUrl)
    }

    private fun reloadTfmSelector(runnableProject: RunnableProject) {
        tfmSelector.stringList.clear()

        runnableProject.projectOutputs.map { it.tfm?.presentableName ?: "" }.sorted().forEach {
            tfmSelector.stringList.add(it)
        }
        if (tfmSelector.stringList.isNotEmpty()) {
            tfmSelector.string.set(tfmSelector.stringList.first())
        }

        handleChangeTfmSelection()
    }

    private fun readLocalSettingsForProject(runnableProject: RunnableProject) {
        functionLocalSettings = FunctionLocalSettingsUtil.readFunctionLocalSettings(runnableProject)
    }

    fun reset(
        projectFilePath: String,
        trackProjectExePath: Boolean,
        trackProjectArguments: Boolean,
        trackProjectWorkingDirectory: Boolean,
        projectTfm: String,
        exePath: String,
        programParameters: String,
        workingDirectory: String,
        functionNames: String,
        envs: Map<String, String>,
        passParentEnvs: Boolean,
        useExternalConsole: Boolean,
        isUnloadedProject: Boolean,
        dotNetStartBrowserParameters: DotNetStartBrowserParameters
    ) {
        fun resetProperties(
            exePath: String,
            programParameters: String,
            workingDirectory: String,
            workerRuntimeSupportsExternalConsole: Boolean
        ) {
            super.reset(
                exePath = exePath,
                programParameters = programParameters,
                workingDirectory = workingDirectory,
                envs = envs,
                isPassParentEnvs = passParentEnvs,
                runtime = null,
                runtimeOptions = "",
                useExternalConsole = workerRuntimeSupportsExternalConsole && useExternalConsole
            )
        }

        isLoaded = false

        this.trackProjectExePath = trackProjectExePath
        this.trackProjectArguments = trackProjectArguments
        this.trackProjectWorkingDirectory = trackProjectWorkingDirectory

        this.functionNamesEditor.defaultValue.value = ""
        this.functionNamesEditor.text.value = functionNames

        this.dotNetBrowserSettingsEditor.settings.set(
            BrowserSettings(
                startAfterLaunch = dotNetStartBrowserParameters.startAfterLaunch,
                withJavaScriptDebugger = dotNetStartBrowserParameters.withJavaScriptDebugger,
                myBrowser = dotNetStartBrowserParameters.browser
            )
        )

        this.urlEditor.defaultValue.value = dotNetStartBrowserParameters.url
        this.urlEditor.text.value = dotNetStartBrowserParameters.url

        runnableProjectsModel.projects.adviseOnce(lifetime) { projectList ->
            if (projectFilePath.isEmpty() || projectList.none {
                    it.projectFilePath == projectFilePath && FunctionRunConfigurationType.isTypeApplicable(it.kind)
                }
            ) {
                // Case when the project wasn't selected, otherwise, we should generate a fake project to avoid dropping user settings.
                if (projectFilePath.isEmpty() || !isUnloadedProject) {
                    projectList.firstOrNull { FunctionRunConfigurationType.isTypeApplicable(it.kind) }?.let { project ->
                        projectSelector.project.set(project)
                        isLoaded = true
                        handleProjectSelection(project)
                    }
                } else {
                    val fakeProjectName = File(projectFilePath).name
                    val fakeProject = RunnableProject(
                        fakeProjectName, fakeProjectName, projectFilePath, RunnableProjectKinds.Unloaded,
                        listOf(
                            ProjectOutput(
                                RdTargetFrameworkId(RdVersionInfo(0, 0, 0), "", projectTfm, false, false), exePath,
                                ParametersListUtil.parse(programParameters), workingDirectory, "", null, emptyList()
                            )
                        ),
                        envs.map { EnvironmentVariable(it.key, it.value) }.toList(), null, listOf()
                    )
                    projectSelector.projectList.apply {
                        clear()
                        addAll(projectList + fakeProject)
                    }
                    projectSelector.project.set(fakeProject)
                    reloadTfmSelector(fakeProject)
                    resetProperties(
                        exePath = exePath,
                        programParameters = programParameters,
                        workingDirectory = workingDirectory,
                        workerRuntimeSupportsExternalConsole = true
                    )
                }
            } else {
                projectList.singleOrNull {
                    it.projectFilePath == projectFilePath && FunctionRunConfigurationType.isTypeApplicable(it.kind)
                }?.let { runnableProject ->
                    projectSelector.project.set(runnableProject)

                    functionLocalSettings = FunctionLocalSettingsUtil.readFunctionLocalSettings(runnableProject)

                    reloadTfmSelector(runnableProject)
                    val projectTfmExists = runnableProject.projectOutputs.any { it.tfm?.presentableName == projectTfm }
                    val selectedTfm =
                        if (projectTfmExists) projectTfm else runnableProject.projectOutputs.firstOrNull()?.tfm?.presentableName
                            ?: ""
                    tfmSelector.string.set(selectedTfm)

                    val projectOutput =
                        runnableProject.projectOutputs.singleOrNull { it.tfm?.presentableName == selectedTfm }
                    val effectiveExePath =
                        if (trackProjectExePath && projectOutput != null) projectOutput.exePath else exePath
                    val effectiveProgramParameters =
                        if (trackProjectArguments && projectOutput != null && projectOutput.defaultArguments.isNotEmpty())
                            ParametersListUtil.join(projectOutput.defaultArguments).replace("\\\"", "\"")
                        else if (programParameters.isNotEmpty())
                            programParameters
                        else
                            programParametersEditor.defaultValue.value

                    programParametersEditor.defaultValue.set(effectiveProgramParameters)

                    val effectiveWorkingDirectory = if (trackProjectWorkingDirectory && projectOutput != null)
                        projectOutput.workingDirectory else workingDirectory


                    // Disable "Use external console" for Isolated worker
                    val workerRuntime = functionLocalSettings?.values?.workerRuntime ?: FunctionWorkerRuntime.DOTNET
                    val workerRuntimeSupportsExternalConsole = workerRuntime != FunctionWorkerRuntime.DOTNET_ISOLATED
                    useExternalConsoleEditor.isVisible.set(workerRuntimeSupportsExternalConsole)

                    resetProperties(
                        exePath = effectiveExePath,
                        programParameters = effectiveProgramParameters,
                        workingDirectory = effectiveWorkingDirectory,
                        workerRuntimeSupportsExternalConsole = workerRuntimeSupportsExternalConsole
                    )

                    composeUrlString(getPortValue())
                }
            }
            isLoaded = true
        }
    }
}