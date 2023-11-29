/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateOrUpdateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.Constants
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.webapp.*
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import org.apache.commons.lang3.StringUtils
import java.awt.Desktop
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL

class WebAppRunState(project: Project, private val webAppConfiguration: WebAppConfiguration) :
    RiderAzureRunProfileState<WebAppBase<*, *, *>>(project) {

    private val webAppPublishModel: WebAppPublishModel = webAppConfiguration.getModel()

    override fun executeSteps(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        OperationContext.current().setMessager(processHandlerMessenger)
//        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
//                .firstOrNull { it.projectFilePath == webAppPublishModel.projectPath }
//                ?: throw RuntimeException("Project is not defined")
//        val artifact = WebAppArtifactService.getInstance(project)
//                .prepareArtifact(publishableProject, webAppPublishModel.projectConfiguration, webAppPublishModel.projectPlatform, processHandler)
//        val deployTarget = getOrCreateDeployTargetFromAppSettingModel(processHandler)
//        updateApplicationSettings(deployTarget, processHandler)
//        AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(deployTarget, artifact, webAppPublishModel.isDeployToRoot, processHandler)
//        FileUtil.delete(artifact)

        val config = createDotNetAppServiceConfig()
        val task = CreateOrUpdateDotNetWebAppTask(config)
        return task.execute()
    }

    private fun createDotNetAppServiceConfig(): DotNetAppServiceConfig {
        return DotNetAppServiceConfig().apply {
            subscriptionId(webAppPublishModel.subscriptionId)
            resourceGroup(webAppPublishModel.resourceGroup)
            appName(webAppPublishModel.webAppName)
            region(Region.fromName(webAppPublishModel.region))
            pricingTier(PricingTier.fromString(webAppPublishModel.pricing))
            servicePlanName(webAppPublishModel.appServicePlanName)
            servicePlanResourceGroup(webAppPublishModel.appServicePlanResourceGroupName)
            runtime(createRuntimeConfig())
            dotnetRuntime = createDotNetRuntimeConfig()
            appSettings(webAppPublishModel.appSettings)
        }
    }

    private fun createRuntimeConfig() = RuntimeConfig().apply {
        os(OperatingSystem.fromString(webAppPublishModel.operatingSystem))
    }

    private fun createDotNetRuntimeConfig() = DotNetRuntimeConfig().apply {
        os(OperatingSystem.fromString(webAppPublishModel.operatingSystem))
        isDocker = false
        stack = "DOTNETCORE"
        version = "8.0"
    }

    private fun getOrCreateDeployTargetFromAppSettingModel(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        val webApp = getOrCreateWebappFromAppSettingModel(processHandler)
        if (!isDeployToSlot()) return webApp

        return if (StringUtils.equals(webAppPublishModel.slotName, Constants.CREATE_NEW_SLOT)) {
            AzureWebAppMvpModel.getInstance().createDeploymentSlotFromSettingModel(webApp, webAppPublishModel)
        } else {
            webApp.slots().get(webAppPublishModel.slotName, webAppPublishModel.resourceGroup)
                ?: throw NullPointerException("Failed to get deployment slot with name ${webAppPublishModel.slotName}")
        }
    }

    private fun getOrCreateWebappFromAppSettingModel(processHandler: RunProcessHandler): WebApp {
        val name = webAppPublishModel.webAppName
        val rg = webAppPublishModel.resourceGroup
        val id = webAppPublishModel.webAppId
        val webapps = Azure.az(AzureWebApp::class.java).webApps(webAppPublishModel.subscriptionId)
        val webApp = if (StringUtils.isNotBlank(id)) webapps.get(id) else webapps.get(name, rg)
        if (webApp != null) return webApp

        if (webAppPublishModel.isCreatingNew) {
            processHandler.setText("Creating new web app...")
            return AzureWebAppMvpModel.getInstance().createWebAppFromSettingModel(webAppPublishModel)
        } else {
            processHandler.setText("Deployment failed!")
            throw Exception("Cannot get webapp for deploy.")
        }
    }

    private fun isDeployToSlot() = !webAppPublishModel.isCreatingNew && webAppPublishModel.isDeployToSlot

    private fun updateApplicationSettings(deployTarget: WebAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val applicationSettings = webAppConfiguration.applicationSettings.toMutableMap()
        val appSettingsToRemove =
            if (webAppConfiguration.isCreatingNew) emptySet()
            else getAppSettingsToRemove(deployTarget, applicationSettings)
        if (applicationSettings.isEmpty()) return

        if (deployTarget is WebApp) {
            processHandler.setText("Updating application settings...")
            val draft = deployTarget.update() as? WebAppDraft ?: return
            appSettingsToRemove.forEach { draft.removeAppSetting(it) }
            draft.appSettings = applicationSettings
            draft.updateIfExist()
            processHandler.setText("Update application settings successfully.")
        } else if (deployTarget is WebAppDeploymentSlot) {
            processHandler.setText("Updating deployment slot application settings...")
            val draft = deployTarget.update() as? WebAppDeploymentSlotDraft ?: return
            appSettingsToRemove.forEach { draft.removeAppSetting(it) }
            draft.appSettings = applicationSettings
            draft.updateIfExist()
            processHandler.setText("Update deployment slot application settings successfully.")
        }
    }

    private fun getAppSettingsToRemove(target: WebAppBase<*, *, *>, applicationSettings: Map<String, String>) =
        target.appSettings
            ?.keys
            ?.asSequence()
            ?.filter { !applicationSettings.containsKey(it) }
            ?.toSet()
            ?: emptySet()

    override fun onSuccess(result: WebAppBase<*, *, *>, processHandler: RunProcessHandler) {
        updateConfigurationDataModel(result)
        processHandler.setText("Deployment was successful but the app may still be starting.")
        val url = "https://${result.hostName}"
        processHandler.setText("URL: $url")
        if (webAppPublishModel.isOpenBrowserAfterDeployment) {
            openWebAppInBrowser(url, processHandler)
        }
        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: WebAppBase<*, *, *>) {
        webAppConfiguration.isCreatingNew = false
        if (app is WebAppDeploymentSlot) {
            webAppConfiguration.slotName = app.name
            webAppConfiguration.newSlotConfigurationSource = AzureWebAppMvpModel.DO_NOT_CLONE_SLOT_CONFIGURATION
            webAppConfiguration.newSlotName = ""
            webAppConfiguration.webAppId = app.parent.id
        } else {
            webAppConfiguration.webAppId = app.id
        }
        webAppConfiguration.applicationSettings = app.appSettings ?: emptyMap()
        webAppConfiguration.webAppName = app.name
        webAppConfiguration.resourceGroup = ""
        webAppConfiguration.appServicePlanName = ""
        webAppConfiguration.appServicePlanResourceGroupName = ""
    }

    private fun openWebAppInBrowser(url: String, processHandler: RunProcessHandler) {
        try {
            Desktop.getDesktop().browse(URL(url).toURI())
        } catch (ex: Exception) {
            when (ex) {
                is IOException,
                is URISyntaxException -> processHandler.println(ex.message, ProcessOutputTypes.STDERR)

                else -> throw ex
            }
        }
    }
}