/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

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
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import javax.swing.JPanel

class WebAppSettingPanel(private val project: Project, configuration: WebAppConfiguration) :
    RiderAzureSettingPanel<WebAppConfiguration>() {

    private val panel: JPanel
    private val webAppPanel = WebAppDeployConfigurationPanel(project)
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

    override fun apply(configuration: WebAppConfiguration) {
        val runConfigurationModel = webAppPanel.value
        val (projectConfiguration, projectPlatform) = webAppPanel.getConfigurationAndPlatform()
        val publishableProject = runConfigurationModel.artifactConfig?.let {
            val artifactId = it.artifactIdentifier.toIntOrNull() ?: return@let null
            project.solution.publishableProjectsModel.publishableProjects.values
                .firstOrNull { p -> p.projectModelId == artifactId }
        }

        configuration.appSettingsKey = appSettingsKey
        runConfigurationModel.webAppConfig?.let {
            configuration.webAppId = it.resourceId
            configuration.subscriptionId = it.subscriptionId
            configuration.resourceGroup = it.resourceGroupName
            configuration.webAppName = it.name
            configuration.runtime = it.runtime
            configuration.applicationSettings = it.appSettings
            configuration.isCreatingNew = it.resourceId.isNullOrEmpty()

            if (configuration.isCreatingNew) {
                configuration.region = it.region.name
                configuration.pricing = it.servicePlan.pricingTier.size
                configuration.appServicePlanName = it.servicePlan.name
                configuration.appServicePlanResourceGroupName = it.servicePlan.resourceGroupName
            } else {
                configuration.appServicePlanName = it.servicePlan?.name
                configuration.appServicePlanResourceGroupName = it.servicePlan?.resourceGroupName
            }

            configuration.isDeployToSlot = it.deploymentSlot != null
            it.deploymentSlot?.let { slot ->
                if (slot.isNewCreate) {
                    configuration.slotName = null
                    configuration.newSlotName = slot.name
                    configuration.newSlotConfigurationSource = slot.configurationSource
                } else {
                    configuration.slotName = slot.name
                    configuration.newSlotName = null
                    configuration.newSlotConfigurationSource = null
                }
            }
        }

        configuration.isSlotPanelVisible = runConfigurationModel.isSlotPanelVisible
        configuration.isOpenBrowserAfterDeployment = runConfigurationModel.isOpenBrowserAfterDeployment
        configuration.projectConfiguration = projectConfiguration
        configuration.projectPlatform = projectPlatform
        configuration.publishableProjectPath = publishableProject?.projectFilePath
    }

    override fun reset(configuration: WebAppConfiguration) {
        if (configuration.webAppId.isNullOrEmpty() && configuration.webAppName.isEmpty()) return

        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { p -> p.projectFilePath == configuration.publishableProjectPath }

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
        val pricingTier =
            if (configuration.pricing.isNotEmpty()) PricingTier.fromString(configuration.pricing) else null
        val plan = AppServicePlanConfig
            .builder()
            .subscriptionId(subscription.id)
            .name(configuration.appServicePlanName)
            .resourceGroupName(resourceGroupName)
            .region(region)
            .os(configuration.runtime?.operatingSystem)
            .pricingTier(pricingTier)
            .build()
        val slotConfig = if (configuration.isDeployToSlot) {
            if (configuration.slotName.isNullOrEmpty())
                DeploymentSlotConfig
                    .builder()
                    .newCreate(true)
                    .name(configuration.newSlotName)
                    .configurationSource(configuration.newSlotConfigurationSource)
                    .build()
            else
                DeploymentSlotConfig
                    .builder()
                    .newCreate(false)
                    .name(configuration.slotName)
                    .build()
        } else null
        val configBuilder = WebAppConfig
            .builder()
            .name(configuration.webAppName)
            .resourceId(configuration.webAppId)
            .subscription(subscription)
            .resourceGroup(resourceGroup)
            .runtime(configuration.runtime)
            .servicePlan(plan)
            .deploymentSlot(slotConfig)
            .appSettings(configuration.applicationSettings)
        val webAppConfig =
            if (configuration.isCreatingNew) configBuilder.region(region).pricingTier(pricingTier).build()
            else configBuilder.build()
        val artifactConfig = AzureArtifactConfig
            .builder()
            .artifactIdentifier(publishableProject?.projectModelId?.toString() ?: "")
            .build()
        val runConfigurationModel = WebAppDeployRunConfigurationModel
            .builder()
            .webAppConfig(webAppConfig)
            .artifactConfig(artifactConfig)
            .slotPanelVisible(configuration.isSlotPanelVisible)
            .openBrowserAfterDeployment(configuration.isOpenBrowserAfterDeployment)
            .build()
        webAppPanel.value = runConfigurationModel
        webAppPanel.setConfigurationAndPlatform(configuration.projectConfiguration, configuration.projectPlatform)
    }

    override fun getMainPanel() = panel

    override fun disposeEditor() {
    }
}