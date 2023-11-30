/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages
import com.azure.resourcemanager.appservice.models.WebAppRuntimeStack
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppModule
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource

class DotNetWebAppDraft : WebApp, AzResource.Draft<WebApp, com.azure.resourcemanager.appservice.models.WebApp> {
    constructor(name: String, resourceGroupName: String, module: WebAppModule) : super(
        name,
        resourceGroupName,
        module
    ) {
        origin = null
    }

    constructor(origin: WebApp) : super(origin) {
        this.origin = origin
    }

    private val origin: WebApp?

    private var config: Config? = null

    private val lock = Any()

    override fun getOrigin() = origin

    override fun reset() {
        config = null
    }

    private fun ensureConfig(): Config {
        synchronized(lock) {
            val localConfig = config ?: Config()
            config = localConfig
            return localConfig
        }
    }

    override fun createResourceInAzure(): com.azure.resourcemanager.appservice.models.WebApp {
        val newRuntime = requireNotNull(dotNetRuntime) { "'runtime' is required to create Azure Web App" }
        val newPlan = requireNotNull(appServicePlan) { "'service plan' is required to create Azure Web App" }
        val os = newRuntime.operatingSystem
        if (os != newPlan.operatingSystem) {
            throw AzureToolkitRuntimeException("Could not create $os app service in ${newPlan.operatingSystem} service plan")
        }
        val newAppSettings = appSettings
        val newDiagnosticConfig = diagnosticConfig

        val manager = requireNotNull(parent.remote)
        val blank = manager.webApps().define(name)
        val withCreate =
            if (!newRuntime.isDocker) createWebApp(blank, os, newPlan, newRuntime)
            else createDockerWebApp(blank, os, newPlan)

        if (newAppSettings != null)
            withCreate.withAppSettings(newAppSettings)
        if (newDiagnosticConfig != null)
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig)

        val messager = AzureMessager.getMessager()
        messager.info(AzureString.format("Start creating Web App({0})...", name))

        val webApp = withCreate.create()

        val deploy = AzureActionManager.getInstance().getAction(DEPLOY)?.bind(this)
        val open = AzureActionManager.getInstance().getAction(AppServiceAppBase.OPEN_IN_BROWSER)?.bind(this)
        messager.success(AzureString.format("Web App({0}) is successfully created", name), deploy, open)

        return webApp
    }

    private fun createWebApp(
        blank: DefinitionStages.Blank,
        os: OperatingSystem,
        plan: AppServicePlan,
        runtime: DotNetRuntime
    ) = when (os) {
        OperatingSystem.LINUX -> {
            val stack = requireNotNull(runtime.stack) { "Unable to configure web app runtime" }
            blank
                .withExistingLinuxPlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withBuiltInImage(stack)
        }

        OperatingSystem.WINDOWS -> {
            val frameworkVersion = requireNotNull(runtime.frameworkVersion) { "Unable to configure web app runtime" }
            blank
                .withExistingWindowsPlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withRuntimeStack(WebAppRuntimeStack.NET)
                .withNetFrameworkVersion(frameworkVersion)
        }

        OperatingSystem.DOCKER -> throw AzureToolkitRuntimeException("Unsupported operating system $os")
    }

    private fun createDockerWebApp(
        blank: DefinitionStages.Blank,
        os: OperatingSystem,
        plan: AppServicePlan
    ): DefinitionStages.WithCreate {
        val dockerConfig =
            requireNotNull(dockerConfiguration) { "Docker configuration is required to create a docker based Azure Web App" }
        val withFramework = when (os) {
            OperatingSystem.LINUX -> {
                blank
                    .withExistingLinuxPlan(plan.remote)
                    .withExistingResourceGroup(resourceGroupName)
            }

            OperatingSystem.WINDOWS -> {
                blank
                    .withExistingWindowsPlan(plan.remote)
                    .withExistingResourceGroup(resourceGroupName)
            }

            OperatingSystem.DOCKER -> throw AzureToolkitRuntimeException("Unsupported operating system $os")
        }

        val draft =
            if (dockerConfig.userName.isNullOrEmpty() && dockerConfig.password.isNullOrEmpty())
                withFramework.withPublicDockerHubImage(dockerConfig.image)
            else if (dockerConfig.registryUrl.isNullOrEmpty())
                withFramework.withPrivateDockerHubImage(dockerConfig.image)
                    .withCredentials(dockerConfig.userName, dockerConfig.password)
            else
                withFramework.withPrivateRegistryImage(dockerConfig.image, dockerConfig.registryUrl)
                    .withCredentials(dockerConfig.userName, dockerConfig.password)

        return draft.withStartUpCommand(dockerConfig.startUpCommand)
    }

    override fun isModified(): Boolean {
        TODO("Not yet implemented")
    }

    override fun updateResourceInAzure(origin: com.azure.resourcemanager.appservice.models.WebApp): com.azure.resourcemanager.appservice.models.WebApp {
        TODO("Not yet implemented")
    }

    var dotNetRuntime: DotNetRuntime?
        get() = config?.runtime ?: remote?.getDotNetRuntime()
        set(value) {
            ensureConfig().runtime = value
        }

    var dockerConfiguration: DockerConfiguration?
        get() = config?.dockerConfiguration
        set(value) {
            ensureConfig().dockerConfiguration = value
        }

    override fun getAppServicePlan() = config?.plan ?: super.getAppServicePlan()
    fun setAppServicePlan(value: AppServicePlan?) {
        ensureConfig().plan = value
    }

    override fun getAppSettings() = config?.appSettings ?: super.getAppSettings()
    fun setAppSettings(value: Map<String, String>?) {
        ensureConfig().appSettings = value
    }

    override fun getDiagnosticConfig() = config?.diagnosticConfig ?: super.getDiagnosticConfig()
    fun setDiagnosticConfig(value: DiagnosticConfig?) {
        ensureConfig().diagnosticConfig = value
    }

    data class Config(
        var runtime: DotNetRuntime? = null,
        var plan: AppServicePlan? = null,
        var diagnosticConfig: DiagnosticConfig? = null,
        var appSettingsToRemove: Set<String> = setOf(),
        var appSettings: Map<String, String>? = null,
        var dockerConfiguration: DockerConfiguration? = null
    )
}