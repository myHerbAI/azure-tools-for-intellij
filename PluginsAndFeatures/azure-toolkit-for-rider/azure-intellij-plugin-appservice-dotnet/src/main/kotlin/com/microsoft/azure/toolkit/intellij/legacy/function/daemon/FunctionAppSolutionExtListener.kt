/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.daemon

import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.project.Project
import com.jetbrains.rd.protocol.SolutionExtListener
import com.jetbrains.rd.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.azure.model.FunctionAppDaemonModel
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution

class FunctionAppSolutionExtListener : SolutionExtListener<FunctionAppDaemonModel> {
    override fun extensionCreated(lifetime: Lifetime, session: ClientProjectSession, model: FunctionAppDaemonModel) {
        model.runFunctionApp.advise(lifetime) {
            runConfiguration(
                functionName = it.functionName,
                runnableProject = getRunnableProject(session.project, it.projectFilePath),
                executor = DefaultRunExecutor.getRunExecutorInstance(),
                project = session.project
            )
        }
        model.debugFunctionApp.advise(lifetime) {
            runConfiguration(
                functionName = it.functionName,
                runnableProject = getRunnableProject(session.project, it.projectFilePath),
                executor = DefaultDebugExecutor.getDebugExecutorInstance(),
                project = session.project
            )
        }
        model.triggerFunctionApp.advise(lifetime) {
//            val triggerAction = TriggerAzureFunctionAction(functionName = triggerFunctionRequest.functionName)
//
//            ActionUtil.invokeAction(
//                triggerAction,
//                SimpleDataContext.getProjectContext(project),
//                ActionPlaces.EDITOR_GUTTER_POPUP,
//                null,
//                null
//            )
        }
    }

    private fun getRunnableProject(project: Project, expectedProjectPath: String): RunnableProject {
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrEmpty()
        return runnableProjects.find { runnableProject ->
            runnableProject.projectFilePath == expectedProjectPath && runnableProject.kind == AzureRunnableProjectKinds.AzureFunctions
        }
            ?: throw IllegalStateException(
                "Unable to find a project to run with path: '$expectedProjectPath', available project paths: " +
                        runnableProjects.joinToString(", ", "'", "'") { it.projectFilePath }
            )
    }

    private fun runConfiguration(
        functionName: String?,
        runnableProject: RunnableProject,
        executor: Executor,
        project: Project
    ) {
        val runManager = RunManager.getInstance(project)
        val existingSettings = findExistingConfigurationSettings(functionName, runnableProject.projectFilePath)

    }

    private fun findExistingConfigurationSettings(
        functionName: String?,
        projectFilePath: String
    ): RunnerAndConfigurationSettings? {
        return null
    }
}