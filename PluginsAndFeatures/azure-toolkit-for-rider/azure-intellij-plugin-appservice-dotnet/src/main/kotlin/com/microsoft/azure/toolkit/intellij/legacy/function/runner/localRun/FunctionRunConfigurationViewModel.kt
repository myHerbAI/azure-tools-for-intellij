/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.RunnableProjectsModel
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.runtimeSelection.RuntimeSelector
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfigurationViewModel
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters

class FunctionRunConfigurationViewModel(
    private val lifetime: Lifetime,
    private val project: Project,
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

    }
}