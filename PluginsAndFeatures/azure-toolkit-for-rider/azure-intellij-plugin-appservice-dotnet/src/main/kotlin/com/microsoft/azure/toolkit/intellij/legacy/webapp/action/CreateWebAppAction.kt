/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_CONFIGURATION
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_PLATFORM
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppArtifactService
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import java.nio.file.Path

class CreateWebAppAction {
    companion object {
        fun openDialog(project: Project, data: WebAppConfig?) {
            AzureTaskManager.getInstance().runLater {
                val dialog = WebAppCreationDialog(project)
                if (data != null) dialog.data = data

                val actionId = Action.Id.of<WebAppConfig>("user/webapp.create_app.app")
                dialog.setOkAction(Action(actionId)
                        .withLabel("Create")
                        .withIdParam(WebAppConfig::getName)
                        .withSource(WebAppConfig::getResourceGroup)
                        .withAuthRequired(true)
                        .withHandler { config ->
                            try {
                                CacheManager.getUsageHistory(WebAppConfig::class.java).push(config)
                                val webApp = WebAppService.getInstance().createWebApp(config)
                                val projectModel = project.solution.publishableProjectsModel.publishableProjects.values
                                        .firstOrNull { Path.of(it.projectFilePath) == config.application }
                                val projectConfiguration = config.appSettings[RIDER_PROJECT_CONFIGURATION]
                                if (projectConfiguration != null) config.appSettings.remove(RIDER_PROJECT_CONFIGURATION)
                                val projectPlatform = config.appSettings[RIDER_PROJECT_PLATFORM]
                                if (projectConfiguration != null) config.appSettings.remove(RIDER_PROJECT_PLATFORM)

                                if (projectModel != null && projectConfiguration != null && projectPlatform != null) {
                                    Action(Action.Id.of<WebApp>("user/webapp.deploy_artifact.app"))
                                            .withIdParam(WebApp::getName)
                                            .withSource { s -> s }
                                            .withAuthRequired(true)
                                            .withHandler { app -> deploy(app, projectModel, projectConfiguration, projectPlatform, project) }
                                            .handle(webApp)
                                }
                            } catch (ex: Exception) {
                                val action = Action(Action.Id.of<Any>("user/webapp.reopen_creation_dialog"))
                                        .withLabel("Reopen dialog ${dialog.title}")
                                        .withHandler { openDialog(project, config) }
                                AzureMessager.getMessager().error(ex, null, action)
                            }
                        }
                )
                dialog.show()
            }
        }

        private fun deploy(
                webapp: WebApp,
                projectModel: PublishableProjectModel,
                projectConfiguration: String,
                projectPlatform: String,
                project: Project
        ) {
            ProgressManager.getInstance().progressIndicator.isIndeterminate = true
            val processHandler = RunProcessHandler()
            processHandler.addDefaultListener()
            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            processHandler.startNotify()
            consoleView.attachToProcess(processHandler)

            val messenger = RunProcessHandlerMessenger(processHandler)
            OperationContext.current().messager = messenger
            val artifact = WebAppArtifactService.getInstance(project)
                    .prepareArtifact(projectModel, projectConfiguration, projectPlatform, processHandler)
            AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(webapp, artifact, true, processHandler)
            FileUtil.delete(artifact)
            AzureMessager.getMessager().success("Successfully deployed artifact to web app (${webapp.name})")
        }
    }
}