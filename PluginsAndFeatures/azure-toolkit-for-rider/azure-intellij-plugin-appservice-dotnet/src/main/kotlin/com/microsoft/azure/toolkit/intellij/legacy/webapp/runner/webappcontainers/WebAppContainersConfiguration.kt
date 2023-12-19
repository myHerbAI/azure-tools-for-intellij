/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DialogTitleCapitalization")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel

class WebAppContainersConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
        RiderAzureRunConfigurationBase<WebAppOnLinuxDeployModel>(project, factory, name) {
    companion object {
        private const val REPO_COMPONENT_REGEX_PATTERN = "[a-z0-9]+(?:[._-][a-z0-9]+)*"
        private const val TAG_REGEX_PATTERN = "^\\w+[\\w.-]*\$"
    }

    private val webAppContainersModel = WebAppOnLinuxDeployModel()

    private val repoComponentRegex = Regex(REPO_COMPONENT_REGEX_PATTERN)
    private val tagRegex = Regex(TAG_REGEX_PATTERN)

    var webAppId: String?
        get() = webAppContainersModel.webAppId
        set(value) {
            webAppContainersModel.webAppId = value
        }
    var webAppName: String?
        get() = webAppContainersModel.webAppName
        set(value) {
            webAppContainersModel.webAppName = value
        }
    private var subscriptionId: String
        get() = webAppContainersModel.subscriptionId
        set(value) {
            webAppContainersModel.subscriptionId = value
        }
    private var resourceGroupName: String
        get() = webAppContainersModel.appServicePlanResourceGroupName
        set(value) {
            webAppContainersModel.resourceGroupName = value
        }
    private var locationName: String?
        get() = webAppContainersModel.locationName
        set(value) {
            webAppContainersModel.locationName = value
        }
    private var pricingSkuTier: String?
        get() = webAppContainersModel.pricingSkuTier
        set(value) {
            webAppContainersModel.pricingSkuTier = value
        }
    private var pricingSkuSize: String?
        get() = webAppContainersModel.pricingSkuSize
        set(value) {
            webAppContainersModel.pricingSkuSize = value
        }
    private var isCreatingNewWebAppOnLinux: Boolean
        get() = webAppContainersModel.isCreatingNewWebAppOnLinux
        set(value) {
            webAppContainersModel.isCreatingNewWebAppOnLinux = value
        }
    private var isCreatingNewAppServicePlan: Boolean
        get() = webAppContainersModel.isCreatingNewAppServicePlan
        set(value) {
            webAppContainersModel.isCreatingNewAppServicePlan = value
        }
    private var appServicePlanName: String?
        get() = webAppContainersModel.appServicePlanName
        set(value) {
            webAppContainersModel.appServicePlanName = value
        }
    private var appServicePlanResourceGroupName: String?
        get() = webAppContainersModel.appServicePlanResourceGroupName
        set(value) {
            webAppContainersModel.appServicePlanResourceGroupName = value
        }
    var finalRepositoryName: String?
        get() = webAppContainersModel.finalRepositoryName
        set(value) {
            webAppContainersModel.finalRepositoryName = value
        }
    var finalTagName: String?
        get() = webAppContainersModel.finalTagName
        set(value) {
            webAppContainersModel.finalTagName = value
        }
    var port: Int
        get() = webAppContainersModel.port
        set(value) {
            webAppContainersModel.port = value
        }

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            WebAppContainersRunState(project, this)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
            WebAppContainersSettingEditor(project)

    override fun getModel() = webAppContainersModel

    override fun checkConfiguration() {
        checkAzurePreconditions()
        validateDockerImageConfiguration()
        with(webAppContainersModel) {
            if (isCreatingNewWebAppOnLinux) {
                if (webAppName.isNullOrEmpty()) throw ConfigurationException("Please specify a Web App for Containers.")
                if (subscriptionId.isNullOrEmpty()) throw ConfigurationException("Please specify a Subscription.")
                if (resourceGroupName.isNullOrEmpty()) throw ConfigurationException("Please specify a Resource Group.")
                if (appServicePlanName.isNullOrEmpty()) throw ConfigurationException("Please specify an App Service Plan.")
            } else {
                if (webAppId.isNullOrEmpty()) throw ConfigurationException("Please specify Web App for Containers.")
            }
        }
    }

    private fun validateDockerImageConfiguration() {
        val repositoryName = finalRepositoryName
        val tagName = finalTagName
        if (repositoryName.isNullOrEmpty()) throw ConfigurationException("Please specify a container registry and an image.")
        if (tagName.isNullOrEmpty()) throw ConfigurationException("Please specify an image tag.")
        if (repositoryName.length > 255) throw ConfigurationException("The length of repository name must be less than 256 characters.")
        val repositoryParts = repositoryName.split('/')
        if (repositoryParts.last().isEmpty()) throw ConfigurationException("Please specify an image name.")
        if (repositoryName.endsWith('/')) throw ConfigurationException("The repository name should not end with '/'.")
        repositoryParts.forEach {
            if (!repoComponentRegex.matches(it)) throw ConfigurationException("Invalid repository component: $it, should follow: $REPO_COMPONENT_REGEX_PATTERN")
        }
        if (tagName.length > 127) throw ConfigurationException("The length of tag name must be less than 128 characters")
        if (!tagRegex.matches(tagName)) throw ConfigurationException("Invalid tag: $tagName, should follow: $TAG_REGEX_PATTERN")
    }

    fun getWebAppConfig(): WebAppConfig {
        val subscription = Subscription(subscriptionId)
        val region = locationName?.let { Region.fromName(it) }
        val rgName = resourceGroupName
        val resourceGroup = ResourceGroupConfig.builder()
                .subscriptionId(subscription.id)
                .name(rgName)
                .region(region)
                .build()
        val pst = pricingSkuTier
        val pss = pricingSkuSize
        val pricingTier =
                if (pst != null && pss != null) PricingTier.fromString(pst, pss)
                else null
        val plan = AppServicePlanConfig.builder()
                .subscriptionId(subscription.id)
                .name(appServicePlanName)
                .resourceGroupName(rgName)
                .region(region)
                .os(OperatingSystem.LINUX)
                .pricingTier(pricingTier)
                .build()
        val configBuilder = WebAppConfig.builder()
                .name(webAppName)
                .resourceId(webAppId)
                .subscription(subscription)
                .resourceGroup(resourceGroup)
                .runtime(Runtime.DOCKER)
                .servicePlan(plan)

        return if (isCreatingNewWebAppOnLinux) configBuilder.region(region).pricingTier(pricingTier).build()
        else configBuilder.build()
    }

    fun getDockerPushConfiguration(): DockerPushConfiguration {
        val imageNameParts = finalRepositoryName?.let {
            val parts = it.split('/', limit = 2)
            if (parts.count() == 2) parts[0] to parts[1] else null
        }
        return DockerPushConfiguration(
                imageNameParts?.first,
                imageNameParts?.second,
                finalTagName,
                null,
                null
        )
    }

    fun setWebAppConfig(webAppConfig: WebAppConfig) {
        webAppId = webAppConfig.resourceId
        subscriptionId = webAppConfig.subscriptionId
        resourceGroupName = webAppConfig.resourceGroupName
        webAppName = webAppConfig.name
        isCreatingNewWebAppOnLinux = webAppConfig.resourceId.isNullOrEmpty()
        if (isCreatingNewWebAppOnLinux) {
            locationName = webAppConfig.region.name
            isCreatingNewAppServicePlan = webAppConfig.servicePlan.toResource().isDraftForCreating
            pricingSkuTier = webAppConfig.servicePlan?.pricingTier?.tier
            pricingSkuSize = webAppConfig.servicePlan?.pricingTier?.size
            appServicePlanName = webAppConfig.servicePlan?.name
            appServicePlanResourceGroupName = webAppConfig.servicePlan?.resourceGroupName
        } else {
            isCreatingNewAppServicePlan = false
            appServicePlanName = webAppConfig.servicePlan?.name
            appServicePlanResourceGroupName = webAppConfig.servicePlan?.resourceGroupName
        }
    }

    fun setDockerPushConfiguration(configuration: DockerPushConfiguration) {
        finalRepositoryName = "${configuration.registryAddress}/${configuration.repositoryName}"
        finalTagName = configuration.tagName
    }
}