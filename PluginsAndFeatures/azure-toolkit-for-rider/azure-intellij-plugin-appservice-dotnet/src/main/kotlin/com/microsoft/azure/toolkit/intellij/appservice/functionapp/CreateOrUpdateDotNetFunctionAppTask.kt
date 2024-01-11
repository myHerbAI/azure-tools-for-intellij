/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.function.*
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import com.microsoft.azure.toolkit.lib.storage.StorageAccount
import com.microsoft.azure.toolkit.lib.storage.StorageAccountDraft
import com.microsoft.azure.toolkit.lib.storage.model.Kind
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.Callable

class CreateOrUpdateDotNetFunctionAppTask(private val config: DotNetFunctionAppConfig) :
    AzureTask<FunctionAppBase<*, *, *>>() {
    companion object {
        private const val FUNCTION_APP_NAME_PATTERN = "[^a-zA-Z0-9]"
    }

    private val functionAppRegex = Regex(FUNCTION_APP_NAME_PATTERN)

    private val subTasks: MutableList<AzureTask<*>> = mutableListOf()

    private lateinit var appServicePlan: AppServicePlan
    private lateinit var storageAccount: StorageAccount
    private lateinit var functionApp: FunctionAppBase<*, *, *>

    init {
        initTasks()
    }

    private fun initTasks() {
        val appDraft = Azure.az(AzureFunctions::class.java)
            .functionApps(config.subscriptionId())
            .updateOrCreate<FunctionAppDraft>(config.appName(), config.resourceGroup())
            .toDotNetFunctionAppDraft()

        registerSubTask(getResourceGroupTask()) {}
        registerSubTask(getServicePlanTask()) { appServicePlan = it }
        if (appDraft.isDraftForCreating) {
            registerSubTask(getStorageAccountTask()) { storageAccount = it }
        }

        if (config.deploymentSlotName().isNullOrEmpty()) {
            val functionTask =
                if (appDraft.exists()) getUpdateFunctionAppTask(appDraft)
                else getCreateFunctionAppTask(appDraft)
            registerSubTask(functionTask) { functionApp = it }
        } else {
            val slotDraft = getFunctionDeploymentSlot(appDraft)
            val slotTask =
                if (slotDraft.exists()) getUpdateFunctionSlotTask(slotDraft)
                else getCreateFunctionSlotTask(slotDraft)
            registerSubTask(slotTask) { functionApp = it }
        }
    }

    private fun FunctionAppDraft.toDotNetFunctionAppDraft(): DotNetFunctionAppDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetFunctionAppDraft(draftOrigin)
            else DotNetFunctionAppDraft(name, resourceGroupName, module as FunctionAppModule)

        return draft
    }

    private fun FunctionAppDeploymentSlotDraft.toDotNetFunctionAppDeploymentSlotDraft(): DotNetFunctionAppDeploymentSlotDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetFunctionAppDeploymentSlotDraft(draftOrigin)
            else DotNetFunctionAppDeploymentSlotDraft(name, module as FunctionAppDeploymentSlotModule)

        return draft
    }

    private fun getResourceGroupTask(): AzureTask<ResourceGroup> =
        CreateResourceGroupTask(config.subscriptionId(), config.resourceGroup(), config.region())

    private fun getServicePlanTask(): AzureTask<AppServicePlan>? {
        if (!config.deploymentSlotName().isNullOrEmpty()) {
            AzureMessager.getMessager().info("Skip update app service plan for deployment slot")
            return null
        }

        return AzureTask<AppServicePlan>(Callable {
            val planConfig = config.servicePlanConfig
            val draft = Azure.az(AzureAppService::class.java)
                .plans(planConfig.subscriptionId)
                .updateOrCreate<AppServicePlanDraft>(planConfig.name, planConfig.resourceGroupName)
            draft.operatingSystem = planConfig.os
            draft.region = planConfig.region
            draft.pricingTier = planConfig.pricingTier
            return@Callable draft.commit()
        })
    }

    private fun getStorageAccountTask(): AzureTask<StorageAccount> =
        AzureTask<StorageAccount>(Callable {
            val storageAccountName = config.storageAccountName() ?: getDefaultStorageAccountName(config.appName())
            val storageResourceGroup = config.storageAccountResourceGroup() ?: config.resourceGroup()
            val accounts = Azure.az(AzureStorageAccount::class.java).accounts(config.subscriptionId())
            val existingAccount = accounts.get(storageAccountName, storageResourceGroup)
            if (existingAccount != null && existingAccount.exists()) {
                return@Callable existingAccount
            }

            val draft = accounts.create<StorageAccountDraft>(storageAccountName, storageResourceGroup)
            draft.setRegion(getNonStageRegion(config.region()))
            draft.setKind(Kind.STORAGE_V2)
            draft.setRedundancy(Redundancy.STANDARD_LRS)
            return@Callable draft.commit()
        })

    private fun getDefaultStorageAccountName(functionAppName: String): String {
        val context = ResourceManagerUtils.InternalRuntimeContext()
        return context.randomResourceName(functionAppName.replace(functionAppRegex, ""), 20)
    }

    private fun getNonStageRegion(region: Region): Region {
        val regionName = region.name
        if (!regionName.contains("stage", true)) {
            return region
        }

        return regionName.let {
            var name = StringUtils.removeIgnoreCase(it, "(stage)")
            name = StringUtils.removeIgnoreCase(name, "stage")
            name = name.trim()
            return@let Region.fromName(name)
        }
    }

    private fun getCreateFunctionAppTask(draft: DotNetFunctionAppDraft): AzureTask<FunctionApp> {
        val title = "Create new app(${config.appName()}) on subscription(${config.subscriptionId()})"
        return AzureTask<FunctionApp>(title, Callable {
            draft.appServicePlan = appServicePlan
            draft.dotNetRuntime = getRuntime(config.dotnetRuntime)
            draft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
            draft.appSettings = config.appSettings()
            draft.diagnosticConfig = config.diagnosticConfig()
            draft.flexConsumptionConfiguration = config.flexConsumptionConfiguration()
            draft.storageAccount = storageAccount

            val result = draft.createIfNotExist()
            Thread.sleep((10 * 1000).toLong())
            result
        })
    }

    private fun getUpdateFunctionAppTask(draft: DotNetFunctionAppDraft): AzureTask<FunctionApp> {
        val title = "Update function app(${config.appName()})"
        return AzureTask<FunctionApp>(title, Callable {
            draft.appServicePlan = appServicePlan
            draft.dotNetRuntime = getRuntime(config.dotnetRuntime)
            draft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
            val appSettingsToRemove = getAppSettingsToRemove(draft.appSettings ?: emptyMap(), config.appSettings())
            draft.appSettings = config.appSettings()
            draft.appSettingsToRemove = appSettingsToRemove
            draft.diagnosticConfig = config.diagnosticConfig()
            draft.flexConsumptionConfiguration = config.flexConsumptionConfiguration()
            draft.storageAccount = storageAccount

            draft.updateIfExist()
        })
    }

    private fun getFunctionDeploymentSlot(functionApp: FunctionApp): DotNetFunctionAppDeploymentSlotDraft {
        if (!functionApp.exists()) {
            throw AzureToolkitRuntimeException("The Function App does not exist. Please make sure the Function App name is correct")
        }
        if (requireNotNull(functionApp.appServicePlan).pricingTier.isFlexConsumption) {
            throw AzureToolkitRuntimeException("Deployment slot is not supported for function app with consumption plan")
        }

        return functionApp
            .slots()
            .updateOrCreate<FunctionAppDeploymentSlotDraft>(config.deploymentSlotName(), config.resourceGroup())
            .toDotNetFunctionAppDeploymentSlotDraft()
    }

    private fun getCreateFunctionSlotTask(draft: DotNetFunctionAppDeploymentSlotDraft): AzureTask<FunctionAppDeploymentSlot> {
        val title = "Create new slot(${config.deploymentSlotName()}) on function app (${config.appName()})"
        return AzureTask<FunctionAppDeploymentSlot>(title, Callable {
            draft.dotNetRuntime = getRuntime(config.dotnetRuntime)
            draft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
            draft.diagnosticConfig = config.diagnosticConfig()
            draft.configurationSource = config.deploymentSlotConfigurationSource()
            draft.appSettings = config.appSettings()

            draft.commit()
        })
    }

    private fun getUpdateFunctionSlotTask(draft: DotNetFunctionAppDeploymentSlotDraft): AzureTask<FunctionAppDeploymentSlot> {
        val title = "Update function deployment slot(${config.deploymentSlotName()})"
        return AzureTask<FunctionAppDeploymentSlot>(title, Callable {
            draft.dotNetRuntime = getRuntime(config.dotnetRuntime)
            draft.dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
            draft.diagnosticConfig = config.diagnosticConfig()
            val appSettingsToRemove = getAppSettingsToRemove(draft.appSettings ?: emptyMap(), config.appSettings())
            draft.appSettings = config.appSettings()
            draft.appSettingsToRemove = appSettingsToRemove

            draft.commit()
        })
    }

    private fun getRuntime(runtimeConfig: DotNetRuntimeConfig?): DotNetRuntime? {
        if (runtimeConfig == null) return null
        if (runtimeConfig.isDocker) return null

        return DotNetRuntime(
            runtimeConfig.os(),
            runtimeConfig.stack,
            runtimeConfig.frameworkVersion,
            runtimeConfig.functionStack,
            false
        )
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

    private fun <T> registerSubTask(task: AzureTask<T>?, consumer: (result: T) -> Unit) {
        if (task != null) {
            subTasks.add(AzureTask<T>(Callable {
                val result = task.body.call()
                consumer(result)
                return@Callable result
            }))
        }
    }

    override fun doExecute(): FunctionAppBase<*, *, *> {
        for (task in subTasks) {
            task.body.call()
        }

        return functionApp
    }
}