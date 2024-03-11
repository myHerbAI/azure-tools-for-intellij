/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft
import com.microsoft.azure.toolkit.lib.appservice.webapp.*
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask

class CreateOrUpdateDotNetWebAppTask(private val config: DotNetAppServiceConfig) : AzureTask<WebAppBase<*, *, *>>() {
    private val subTasks: List<AzureTask<*>>

    init {
        subTasks = initTasks()
    }

    private fun initTasks(): List<AzureTask<*>> = listOf(
        AzureTask(AzureString.format("Create new web app({0})", config.appName()), ::createOrUpdateResource)
    )

    private fun createOrUpdateResource(): WebAppBase<*, *, *> {
        val az = Azure.az(AzureWebApp::class.java)
        val target = az.webApps(config.subscriptionId()).getOrDraft(config.appName(), config.resourceGroup())
        if (config.deploymentSlotName().isNullOrEmpty()) {
            if (!target.exists()) {
                val availability = az.get(config.subscriptionId(), null)?.checkNameAvailability(config.appName())
                if (availability?.isAvailable != true) {
                    throw AzureToolkitRuntimeException(
                        AzureString.format(
                            "Cannot create webapp {0} due to error: {1}",
                            config.appName(),
                            availability?.unavailabilityReason
                        ).string
                    )
                }
                return create()
            } else {
                return update(target)
            }
        } else {
            if (!target.exists()) {
                throw AzureToolkitRuntimeException("Target Web App does not exist. Please make sure the Web App name is correct")
            }

            val slotDraft = target.slots()
                .updateOrCreate<WebAppDeploymentSlotDraft>(config.deploymentSlotName(), config.resourceGroup())
                .toDotNetWebAppDeploymentSlotDraft()

            return if (slotDraft.exists()) updateDeploymentSlot(slotDraft)
            else createDeploymentSlot(slotDraft)
        }
    }

    private fun create(): WebApp {
        CreateResourceGroupTask(config.subscriptionId(), config.resourceGroup(), config.region()).doExecute()

        val planConfig = AppServiceConfig.getServicePlanConfig(config)
        val planDraft = Azure.az(AzureAppService::class.java)
            .plans(planConfig.subscriptionId)
            .updateOrCreate<AppServicePlanDraft>(planConfig.name, planConfig.resourceGroupName)
        planDraft.planConfig = planConfig

        val appDraft = Azure.az(AzureWebApp::class.java)
            .webApps(config.subscriptionId())
            .create<WebAppDraft>(config.appName(), config.resourceGroup())
            .toDotNetWebAppDraft()

        appDraft.appServicePlan = planDraft.commit()
        appDraft.dotNetRuntime = getRuntime(config.dotnetRuntime)
        appDraft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
        appDraft.diagnosticConfig = config.diagnosticConfig()
        appDraft.appSettingsToRemove = emptySet()
        appDraft.appSettings = config.appSettings()

        return appDraft.createIfNotExist()
    }

    private fun update(webApp: WebApp): WebApp {
        val planConfig = AppServiceConfig.getServicePlanConfig(config)
        val planDraft = Azure.az(AzureAppService::class.java)
            .plans(planConfig.subscriptionId)
            .updateOrCreate<AppServicePlanDraft>(planConfig.name, planConfig.resourceGroupName)
        planDraft.planConfig = planConfig

        val appDraft = (webApp.update() as WebAppDraft).toDotNetWebAppDraft()

        appDraft.appServicePlan = planDraft.commit()
        appDraft.dotNetRuntime = getRuntime(config.dotnetRuntime)
        appDraft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
        appDraft.diagnosticConfig = config.diagnosticConfig()
        appDraft.appSettingsToRemove = getAppSettingsToRemove(webApp.appSettings ?: emptyMap(), config.appSettings())
        appDraft.appSettings = config.appSettings()

        val result = appDraft.updateIfExist()
            ?: throw AzureToolkitRuntimeException("Unable to update Web App")

        return result
    }

    private fun WebAppDraft.toDotNetWebAppDraft(): DotNetWebAppDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetWebAppDraft(draftOrigin)
            else DotNetWebAppDraft(name, resourceGroupName, module as WebAppModule)

        return draft
    }

    private fun WebAppDeploymentSlotDraft.toDotNetWebAppDeploymentSlotDraft(): DotNetWebAppDeploymentSlotDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetWebAppDeploymentSlotDraft(draftOrigin)
            else DotNetWebAppDeploymentSlotDraft(name, module as WebAppDeploymentSlotModule)

        return draft
    }

    private fun createDeploymentSlot(draft: DotNetWebAppDeploymentSlotDraft): WebAppDeploymentSlot {
        draft.dotNetRuntime = getRuntime(config.dotnetRuntime)
        draft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
        draft.diagnosticConfig = config.diagnosticConfig()
        draft.configurationSource = config.deploymentSlotConfigurationSource()
        draft.appSettingsToRemove = emptySet()
        draft.appSettings = config.appSettings()

        return draft.commit()
    }

    private fun updateDeploymentSlot(draft: DotNetWebAppDeploymentSlotDraft): WebAppDeploymentSlot {
        draft.dotNetRuntime = getRuntime(config.dotnetRuntime)
        draft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
        draft.diagnosticConfig = config.diagnosticConfig()
        draft.appSettingsToRemove = getAppSettingsToRemove(draft.appSettings ?: emptyMap(), config.appSettings())
        draft.appSettings = config.appSettings()

        return draft.commit()
    }

    private fun getRuntime(runtimeConfig: DotNetRuntimeConfig?): DotNetRuntime? {
        if (runtimeConfig == null) return null

        return if (!runtimeConfig.isDocker) {
            DotNetRuntime(
                runtimeConfig.os(),
                runtimeConfig.stack,
                runtimeConfig.frameworkVersion,
                null,
                false
            )
        } else {
            DotNetRuntime(
                runtimeConfig.os(),
                null,
                null,
                null,
                true
            )
        }
    }

    private fun getDockerConfiguration(runtimeConfig: DotNetRuntimeConfig?): DockerConfiguration? {
        if (runtimeConfig == null || !runtimeConfig.isDocker) return null

        return DockerConfiguration.builder()
            .userName(runtimeConfig.username())
            .password(runtimeConfig.password())
            .registryUrl(runtimeConfig.registryUrl())
            .image(runtimeConfig.image())
            .startUpCommand(runtimeConfig.startUpCommand())
            .build()
    }

    private fun getAppSettingsToRemove(targetSettings: Map<String, String>, newSettings: Map<String, String>) =
        targetSettings.keys.filter { !newSettings.containsKey(it) }.toSet()

    override fun doExecute(): WebAppBase<*, *, *> {
        var result: Any? = null
        for (task in subTasks) {
            try {
                result = task.body.call()
            } catch (e: Throwable) {
                throw AzureToolkitRuntimeException(e)
            }
        }

        return result as WebAppBase<*, *, *>
    }
}