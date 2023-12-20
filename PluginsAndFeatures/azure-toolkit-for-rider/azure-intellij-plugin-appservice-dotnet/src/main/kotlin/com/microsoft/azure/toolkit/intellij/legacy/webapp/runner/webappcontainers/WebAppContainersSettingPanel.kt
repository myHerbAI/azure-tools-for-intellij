/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.common.*
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import javax.swing.JLabel
import javax.swing.JPanel

class WebAppContainersSettingPanel(private val project: Project) :
    RiderAzureSettingPanel<WebAppContainersConfiguration>() {

    private val panel: JPanel
    private lateinit var containerRegistryComboBox: Cell<AzureContainerRegistryComboBox>
    private lateinit var repositoryLabel: Cell<JLabel>
    private lateinit var repositoryTextField: Cell<JBTextField>
    private lateinit var tagTextField: Cell<JBTextField>
    private lateinit var webAppContainersComboBox: Cell<WebAppContainersComboBox>
    private lateinit var portSpinner: Cell<JBIntSpinner>

    init {
        panel = panel {
            row("Container Registry:") {
                containerRegistryComboBox = dockerContainerRegistryComboBox()
                    .align(Align.FILL)
            }
            row("Repository:") {
                repositoryLabel = label("")
                repositoryTextField = textField()
                label("Tag:")
                tagTextField = textField().columns(COLUMNS_TINY)
            }
            row("Web App:") {
                webAppContainersComboBox = dockerWebAppComboBox(project)
                    .align(Align.FILL)
            }
            row("Website Port:") {
                portSpinner = spinner(80..65535)
            }
        }

        tagTextField.component.text = "latest"
        containerRegistryComboBox.component.addValueChangedListener(::onRegistryChanged)
        webAppContainersComboBox.component.reloadItems()
    }

    override fun apply(configuration: WebAppContainersConfiguration) {
        val webappConfig = webAppContainersComboBox.component.value
        webappConfig?.let {
            configuration.webAppId = it.resourceId
            configuration.subscriptionId = it.subscriptionId
            configuration.resourceGroupName = it.resourceGroupName
            configuration.webAppName = it.name
            configuration.isCreatingNewWebAppOnLinux = it.resourceId.isNullOrEmpty()

            if (configuration.isCreatingNewWebAppOnLinux) {
                configuration.locationName = it.region.name
                configuration.pricingSkuTier = it.servicePlan?.pricingTier?.tier
                configuration.pricingSkuSize = it.servicePlan?.pricingTier?.size ?: ""
                configuration.appServicePlanName = it.servicePlan?.name
                configuration.appServicePlanResourceGroupName = it.servicePlan?.resourceGroupName
            } else {
                configuration.appServicePlanName = it.servicePlan?.name
                configuration.appServicePlanResourceGroupName = it.servicePlan?.resourceGroupName
            }
        }

        val registry = containerRegistryComboBox.component.value
        val repository = repositoryTextField.component.text
        configuration.finalRepositoryName = "${registry?.address}/$repository"
        configuration.finalTagName = tagTextField.component.text

        configuration.port = portSpinner.component.number
    }

    override fun reset(configuration: WebAppContainersConfiguration) {
        if (configuration.webAppId.isNullOrEmpty() && configuration.webAppName.isEmpty()) return

        val subscription = Subscription(configuration.subscriptionId)
        val region = if (configuration.locationName.isNotEmpty()) Region.fromName(configuration.locationName) else null
        val resourceGroupName = configuration.resourceGroupName
        val resourceGroup = ResourceGroupConfig.builder()
            .subscriptionId(subscription.id)
            .name(resourceGroupName)
            .region(region)
            .build()
        val pst = configuration.pricingSkuTier
        val pss = configuration.pricingSkuSize
        val pricingTier =
            if (!pst.isNullOrEmpty() && pss.isNotEmpty()) PricingTier.fromString(pst, pss)
            else null
        val plan = AppServicePlanConfig.builder()
            .subscriptionId(subscription.id)
            .name(configuration.appServicePlanName)
            .resourceGroupName(resourceGroupName)
            .region(region)
            .os(OperatingSystem.LINUX)
            .pricingTier(pricingTier)
            .build()
        val configBuilder = WebAppConfig.builder()
            .name(configuration.webAppName)
            .resourceId(configuration.webAppId)
            .subscription(subscription)
            .resourceGroup(resourceGroup)
            .runtime(Runtime.DOCKER)
            .servicePlan(plan)
        val webAppConfig =
            if (configuration.isCreatingNewWebAppOnLinux) configBuilder.region(region).pricingTier(pricingTier).build()
            else configBuilder.build()
        webAppContainersComboBox.component.value = webAppConfig

        val imageNameParts = configuration.finalRepositoryName?.let {
            val parts = it.split('/', limit = 2)
            if (parts.count() == 2) parts[0] to parts[1] else null
        }
        imageNameParts?.first?.let { containerRegistryComboBox.component.setRegistry(it) }
        repositoryTextField.component.text = imageNameParts?.second
        tagTextField.component.text = configuration.finalTagName

        portSpinner.component.number = configuration.port
    }

    private fun onRegistryChanged(value: ContainerRegistryModel) {
        repositoryLabel.component.text = "${value.address}/"
    }

    override fun getMainPanel() = panel

    override fun disposeEditor() {
    }
}