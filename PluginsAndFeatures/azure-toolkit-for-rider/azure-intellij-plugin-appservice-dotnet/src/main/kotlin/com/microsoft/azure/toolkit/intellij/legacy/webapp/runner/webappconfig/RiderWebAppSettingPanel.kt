package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.ide.appservice.model.AzureArtifactConfig
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppDeployRunConfigurationModel
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import javax.swing.JPanel

class RiderWebAppSettingPanel(private val project: Project, configuration: RiderWebAppConfiguration) : RiderAzureSettingPanel<RiderWebAppConfiguration>() {

    private val panel: JPanel
    private val webAppPanel = RiderWebAppDeployConfigurationPanel(project)
    private var appSettingsKey: String = configuration.appSettingsKey

    init {
        panel = panel {
            row {
                cell(webAppPanel.panel)
                        .align(Align.FILL)
                        .resizableColumn()
            }
        }
    }

    override fun apply(configuration: RiderWebAppConfiguration) {
        val runConfigurationModel = webAppPanel.value
        configuration.appSettingsKey = appSettingsKey
        runConfigurationModel.webAppConfig?.let {
            configuration.webAppId = it.resourceId
            configuration.subscriptionId = it.subscriptionId
            configuration.resourceGroup = it.resourceGroupName
            configuration.webAppName = it.name
            configuration.saveRuntime(it.runtime)
            configuration.isCreatingNew = it.resourceId.isNullOrEmpty()

            if (configuration.isCreatingNew) {
                configuration.region = it.region.name
                configuration.isCreatingAppServicePlan = it.servicePlan.toResource().isDraftForCreating
                configuration.pricing = it.servicePlan.pricingTier.size
                configuration.appServicePlanName = it.servicePlan.name
                configuration.appServicePlanResourceGroupName = it.servicePlan.resourceGroupName
            } else {
                configuration.isCreatingAppServicePlan = false
                configuration.appServicePlanName = it.servicePlan?.name
                configuration.appServicePlanResourceGroupName = it.servicePlan?.resourceGroupName
            }
        }
        runConfigurationModel.artifactConfig?.let {
            val artifactId = it.artifactIdentifier.toIntOrNull()
            if (artifactId != null) {
                project.solution.publishableProjectsModel.publishableProjects.values
                        .firstOrNull { p -> p.projectModelId == artifactId }
                        ?.let { p -> configuration.saveProject(p) }
            }
        }
        configuration.isSlotPanelVisible = runConfigurationModel.isSlotPanelVisible
        configuration.isOpenBrowserAfterDeployment = runConfigurationModel.isOpenBrowserAfterDeployment
    }

    override fun reset(configuration: RiderWebAppConfiguration) {
        if (configuration.webAppId.isNullOrEmpty() && configuration.webAppName.isEmpty()) return

        appSettingsKey = configuration.appSettingsKey
        val subscription = Subscription(configuration.subscriptionId)
        val region = if (configuration.region.isNotEmpty()) Region.fromName(configuration.region) else null
        val resourceGroupName = configuration.resourceGroup
        val resourceGroup = ResourceGroupConfig
                .builder()
                .subscriptionId(subscription.id)
                .name(resourceGroupName)
                .region(region)
                .build()
        val pricingTier = if (configuration.pricing.isNotEmpty()) PricingTier.fromString(configuration.pricing) else null
        val runtime = configuration.operatingSystem?.let { Runtime(it, null, null) }
        val plan = AppServicePlanConfig
                .builder()
                .subscriptionId(subscription.id)
                .name(configuration.appServicePlanName)
                .resourceGroupName(resourceGroupName)
                .region(region)
                .os(configuration.operatingSystem)
                .pricingTier(pricingTier)
                .build()
        val slotConfig: DeploymentSlotConfig? = null
        val configBuilder = WebAppConfig
                .builder()
                .name(configuration.webAppName)
                .resourceId(configuration.webAppId)
                .subscription(subscription)
                .resourceGroup(resourceGroup)
                .runtime(runtime)
                .servicePlan(plan)
                .deploymentSlot(slotConfig)
        val webAppConfig =
                if (configuration.isCreatingNew) configBuilder.region(region).pricingTier(pricingTier).build()
                else configBuilder.build()
        val artifactConfig = AzureArtifactConfig
                .builder()
                .artifactIdentifier(configuration.artifactIdentifier)
                .build()
        val runConfigurationModel = WebAppDeployRunConfigurationModel
                .builder()
                .webAppConfig(webAppConfig)
                .artifactConfig(artifactConfig)
                .openBrowserAfterDeployment(configuration.isOpenBrowserAfterDeployment)
                .build()
        webAppPanel.value = runConfigurationModel
    }

    override fun disposeEditor() {
    }

    override fun getMainPanel() = panel
}