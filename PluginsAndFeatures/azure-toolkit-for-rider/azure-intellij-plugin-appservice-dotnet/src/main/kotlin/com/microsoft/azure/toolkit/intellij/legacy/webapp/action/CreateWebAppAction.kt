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
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateOrUpdateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_CONFIGURATION
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_PLATFORM
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppArtifactService
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.getStackAndVersion
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
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
                            val publishableProject =
                                project.solution.publishableProjectsModel.publishableProjects.values
                                    .firstOrNull { Path.of(it.projectFilePath) == config.application }
                                    ?: throw RuntimeException("Project is not defined")
                            val projectConfiguration = config.appSettings[RIDER_PROJECT_CONFIGURATION]
                            if (projectConfiguration != null) config.appSettings.remove(RIDER_PROJECT_CONFIGURATION)
                            val projectPlatform = config.appSettings[RIDER_PROJECT_PLATFORM]
                            if (projectConfiguration != null) config.appSettings.remove(RIDER_PROJECT_PLATFORM)

                            val appServiceConfig = createDotNetAppServiceConfig(config, publishableProject, project)
                            val createTask = CreateOrUpdateDotNetWebAppTask(appServiceConfig)
                            val deployTarget = createTask.execute() as? WebApp

                            if (deployTarget != null && projectConfiguration != null && projectPlatform != null) {
                                Action(Action.Id.of<WebApp>("user/webapp.deploy_artifact.app"))
                                    .withIdParam(WebApp::getName)
                                    .withSource { s -> s }
                                    .withAuthRequired(true)
                                    .withHandler { app ->
                                        deploy(
                                            app,
                                            publishableProject,
                                            projectConfiguration,
                                            projectPlatform,
                                            project
                                        )
                                    }
                                    .handle(deployTarget)
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

        private fun createDotNetAppServiceConfig(
            config: WebAppConfig,
            publishableProject: PublishableProjectModel,
            project: Project
        ) = DotNetAppServiceConfig().apply {
            subscriptionId(config.subscriptionId)
            resourceGroup(config.resourceGroupName)
            region(config.region)
            servicePlanName(config.servicePlan.name)
            servicePlanResourceGroup(config.servicePlan.resourceGroupName)
            pricingTier(config.servicePlan.pricingTier ?: config.pricingTier)
            appName(config.name)
            runtime(createRuntimeConfig(config))
            dotnetRuntime = createDotNetRuntimeConfig(config, publishableProject, project)
            appSettings(config.appSettings)
            config.deploymentSlot?.let {
                deploymentSlotName(it.name)
                deploymentSlotConfigurationSource(it.configurationSource)
            }
        }

        private fun createRuntimeConfig(config: WebAppConfig) = RuntimeConfig().apply {
            os(config.runtime.operatingSystem)
            javaVersion(JavaVersion.OFF)
            webContainer(WebContainer.JAVA_OFF)
        }

        private fun createDotNetRuntimeConfig(
            config: WebAppConfig,
            publishableProject: PublishableProjectModel,
            project: Project
        ) = DotNetRuntimeConfig().apply {
            val operatingSystem = config.runtime.operatingSystem
            os(operatingSystem)
            javaVersion(JavaVersion.OFF)
            webContainer(WebContainer.JAVA_OFF)
            isDocker = false
            val stackAndVersion = publishableProject.getStackAndVersion(project, operatingSystem)
            stack = stackAndVersion?.first
            frameworkVersion = stackAndVersion?.second
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

            val zipFile = WebAppArtifactService.getInstance(project)
                .prepareArtifact(projectModel, projectConfiguration, projectPlatform, processHandler)
            val artifact = WebAppArtifact.builder()
                .file(zipFile)
                .deployType(DeployType.ZIP)
                .build()
            val deployTask = DeployWebAppTask(webapp, listOf(artifact), true, false, false)
            deployTask.execute()

            FileUtil.delete(zipFile)

            AzureMessager.getMessager().success("Successfully deployed artifact to web app (${webapp.name})")
        }
    }
}