package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
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
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel

class WebAppContainersConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
        RiderAzureRunConfigurationBase<WebAppOnLinuxDeployModel>(project, factory, name) {

    private val webAppContainersModel = WebAppOnLinuxDeployModel()

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
    var subscriptionId: String
        get() = webAppContainersModel.subscriptionId
        set(value) {
            webAppContainersModel.subscriptionId = value
        }
    var isCreatingNewResourceGroup: Boolean
        get() = webAppContainersModel.isCreatingNewResourceGroup
        set(value) {
            webAppContainersModel.isCreatingNewResourceGroup = value
        }
    var resourceGroupName: String
        get() = webAppContainersModel.appServicePlanResourceGroupName
        set(value) {
            webAppContainersModel.resourceGroupName = value
        }
    var locationName: String?
        get() = webAppContainersModel.locationName
        set(value) {
            webAppContainersModel.locationName = value
        }
    var pricingSkuTier: String?
        get() = webAppContainersModel.pricingSkuTier
        set(value) {
            webAppContainersModel.pricingSkuTier = value
        }
    var pricingSkuSize: String?
        get() = webAppContainersModel.pricingSkuSize
        set(value) {
            webAppContainersModel.pricingSkuSize = value
        }
    var privateRegistryImageSetting: PrivateRegistryImageSetting
        get() = webAppContainersModel.privateRegistryImageSetting
        set(value) {
            webAppContainersModel.privateRegistryImageSetting = value
        }
    var isCreatingNewWebAppOnLinux: Boolean
        get() = webAppContainersModel.isCreatingNewWebAppOnLinux
        set(value) {
            webAppContainersModel.isCreatingNewWebAppOnLinux = value
        }
    var isCreatingNewAppServicePlan: Boolean
        get() = webAppContainersModel.isCreatingNewAppServicePlan
        set(value) {
            webAppContainersModel.isCreatingNewAppServicePlan = value
        }
    var appServicePlanName: String?
        get() = webAppContainersModel.appServicePlanName
        set(value) {
            webAppContainersModel.appServicePlanName = value
        }
    var appServicePlanResourceGroupName: String?
        get() = webAppContainersModel.appServicePlanResourceGroupName
        set(value) {
            webAppContainersModel.appServicePlanResourceGroupName = value
        }
    var targetPath: String?
        get() = webAppContainersModel.targetPath
        set(value) {
            webAppContainersModel.targetPath = value
        }
    var targetName: String?
        get() = webAppContainersModel.targetName
        set(value) {
            webAppContainersModel.targetName = value
        }
    var dockerFilePath: String?
        get() = webAppContainersModel.dockerFilePath
        set(value) {
            webAppContainersModel.dockerFilePath = value
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

    override fun validate() {
        checkAzurePreconditions()
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