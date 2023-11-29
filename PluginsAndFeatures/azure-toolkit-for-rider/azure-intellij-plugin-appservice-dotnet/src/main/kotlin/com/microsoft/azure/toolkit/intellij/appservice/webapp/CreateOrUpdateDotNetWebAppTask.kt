/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft
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
        if (!isDeployToDeploymentSlot()) {
            if (!target.exists()) {
                return create()
            } else {
                throw NotImplementedError()
            }
        } else {
            throw NotImplementedError()
        }
    }

    private fun create(): WebApp {
        val region = config.region()
        val planConfig = config.servicePlanConfig

        CreateResourceGroupTask(config.subscriptionId(), config.resourceGroup(), region).doExecute()

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
        appDraft.appSettings = config.appSettings()

        return appDraft.createIfNotExist()
    }

    private fun update(webApp: WebApp): WebApp {
        throw NotImplementedError()
    }

    private fun getRuntime(runtimeConfig: DotNetRuntimeConfig?): DotNetRuntime? {
        if (runtimeConfig == null) return null

        return if (!runtimeConfig.isDocker) {
            DotNetRuntime(runtimeConfig.os(), runtimeConfig.stack, runtimeConfig.version, false)
        } else {
            DotNetRuntime(runtimeConfig.os(), null, null, true)
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

    private fun isDeployToDeploymentSlot() = !config.deploymentSlotName().isNullOrEmpty()

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