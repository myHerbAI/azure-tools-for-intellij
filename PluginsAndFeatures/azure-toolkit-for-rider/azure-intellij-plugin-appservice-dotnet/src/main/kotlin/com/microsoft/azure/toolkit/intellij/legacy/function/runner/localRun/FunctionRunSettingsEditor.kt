/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.ProtocolLifetimedSettingsEditor
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import javax.swing.JComponent

class FunctionRunSettingsEditor(private val project: Project): ProtocolLifetimedSettingsEditor<FunctionRunConfiguration>() {

    lateinit var viewModel: FunctionRunConfigurationViewModel

    override fun createEditor(lifetime: Lifetime): JComponent {
        viewModel = FunctionRunConfigurationViewModel(
            lifetime,
            project,
            project.solution.runnableProjectsModel,
            ProjectSelector("Project:", "Project"),
            StringSelector("Target framework:", "Target_framework"),
            ProgramParametersEditor("Function host arguments:", "Program_arguments", lifetime),
            PathSelector("Working directory:", "Working_directory", FileChooserDescriptorFactory.createSingleFolderDescriptor(), lifetime),
            TextEditor("Function names to run", "Function_name", lifetime),
            EnvironmentVariablesEditor("Environment variables:", "Environment_variables"),
            FlagEditor("Use external console", "Use_external_console"),
            ViewSeparator("Open browser"),
            TextEditor("URL", "URL", lifetime),
            BrowserSettingsEditor("")
        )
        return ControlViewBuilder(lifetime, project).build(viewModel)
    }

    override fun applyEditorTo(configuration: FunctionRunConfiguration) {
        val selectedProject = viewModel.projectSelector.project.valueOrNull
        val selectedTfm = viewModel.tfmSelector.string.valueOrNull
        if (selectedProject != null) {
            configuration.parameters.projectFilePath = selectedProject.projectFilePath
            if (selectedTfm != null) {
                configuration.parameters.apply {
                    projectKind = selectedProject.kind
                    trackProjectExePath = viewModel.trackProjectExePath
                    trackProjectArguments = viewModel.trackProjectArguments
                    trackProjectWorkingDirectory = viewModel.trackProjectWorkingDirectory
                    projectTfm = selectedTfm
                    functionNames = viewModel.functionNamesEditor.text.value
                    exePath = FileUtil.toSystemIndependentName(viewModel.exePathSelector.path.value)
                    programParameters = viewModel.programParametersEditor.parametersString.value
                    workingDirectory = FileUtil.toSystemIndependentName(viewModel.workingDirectorySelector.path.value)
                    envs = viewModel.environmentVariablesEditor.envs.value
                    isPassParentEnvs = viewModel.environmentVariablesEditor.isPassParentEnvs.value
                    useExternalConsole = viewModel.useExternalConsoleEditor.isSelected.value

                    startBrowserParameters.url = viewModel.urlEditor.text.value
                    startBrowserParameters.browser = viewModel.dotNetBrowserSettingsEditor.settings.value.myBrowser
                    startBrowserParameters.startAfterLaunch = viewModel.dotNetBrowserSettingsEditor.settings.value.startAfterLaunch
                    startBrowserParameters.withJavaScriptDebugger = viewModel.dotNetBrowserSettingsEditor.settings.value.withJavaScriptDebugger
                }
            }
        }
    }

    override fun resetEditorFrom(configuration: FunctionRunConfiguration) {
        configuration.parameters.apply {
            viewModel.reset(
                projectFilePath,
                trackProjectExePath,
                trackProjectArguments,
                trackProjectWorkingDirectory,
                projectTfm,
                exePath,
                programParameters,
                workingDirectory,
                functionNames,
                envs,
                isPassParentEnvs,
                useExternalConsole,
                isUnloadedProject,
                startBrowserParameters
            )
        }
    }
}