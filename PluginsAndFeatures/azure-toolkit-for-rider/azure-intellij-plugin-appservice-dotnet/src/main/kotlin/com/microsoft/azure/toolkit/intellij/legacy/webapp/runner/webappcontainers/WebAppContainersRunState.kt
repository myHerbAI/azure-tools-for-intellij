/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService

class WebAppContainersRunState(project: Project, private val webAppContainersConfiguration: WebAppContainersConfiguration)
    : RiderAzureRunProfileState<AppServiceAppBase<*, *, *>>(project) {
    companion object {
        private const val WEBSITES_PORT = "WEBSITES_PORT"
    }

    private val webAppContainersModel = webAppContainersConfiguration.getModel()

    override fun executeSteps(processHandler: RunProcessHandler): AppServiceAppBase<*, *, *> {
        OperationContext.current().setMessager(processHandlerMessenger)
        //push image


        //deploy
        val webAppConfig = webAppContainersConfiguration.getWebAppConfig()
        val appSettings = webAppConfig.appSettings?.toMutableMap() ?: mutableMapOf()
        appSettings[WEBSITES_PORT] = webAppContainersConfiguration.port.toString()
        webAppConfig.appSettings = appSettings
        val appServiceConfig = WebAppService.convertToTaskConfig(webAppConfig)
        Azure.az(AzureWebApp::class.java)
                .webApps(appServiceConfig.subscriptionId())
                .getOrDraft(appServiceConfig.appName(), appServiceConfig.resourceGroup())

        val runtime = appServiceConfig.runtime()
        with(runtime) {
            image("${webAppContainersConfiguration.finalRepositoryName}:${webAppContainersConfiguration.finalTagName}")
        }

        val task = CreateOrUpdateWebAppTask(appServiceConfig)
        return task.execute()
    }

    override fun onSuccess(result: AppServiceAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val imageRepository = webAppContainersModel.finalRepositoryName
        val imageTag = webAppContainersModel.finalTagName

        processHandler.setText("Image $imageRepository:$imageTag has been deployed to Web App ${result.name}")
        val url = "https://${result.name}.azurewebsites.net/"
        processHandler.setText("URL: $url")
        processHandler.notifyComplete()
        updateConfigurationDataModel(result)
    }

    private fun updateConfigurationDataModel(app: AppServiceAppBase<*, *, *>) {
        webAppContainersModel.apply {
            isCreatingNewWebAppOnLinux = false
            webAppId = app.id
            resourceGroupName = app.resourceGroupName
        }
    }
}