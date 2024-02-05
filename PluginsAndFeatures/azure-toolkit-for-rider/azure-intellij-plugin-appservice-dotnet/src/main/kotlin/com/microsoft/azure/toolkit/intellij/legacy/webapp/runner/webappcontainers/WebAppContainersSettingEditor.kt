/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.common.AzureContainerRegistryComboBox
import com.microsoft.azure.toolkit.intellij.common.ContainerRegistryModel
import com.microsoft.azure.toolkit.intellij.common.dockerContainerRegistryComboBox
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import javax.swing.JLabel
import javax.swing.JPanel

class WebAppContainersSettingEditor(private val project: Project) :
        SettingsEditor<WebAppContainersConfiguration>() {

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
                    .resizableColumn()
            }
            row("Repository:") {
                repositoryLabel = label("")
                repositoryTextField = textField()
                label("Tag:")
                tagTextField = textField()
                    .columns(COLUMNS_TINY)
            }
            row("Web App:") {
                webAppContainersComboBox = dockerWebAppComboBox(project)
                    .align(Align.FILL)
                Disposer.register(this@WebAppContainersSettingEditor, webAppContainersComboBox.component)
            }
            row("Website Port:") {
                portSpinner = spinner(80..65535)
            }
        }

        tagTextField.component.text = "latest"
        containerRegistryComboBox.component.addValueChangedListener(::onRegistryChanged)
        webAppContainersComboBox.component.reloadItems()
    }

    private fun onRegistryChanged(value: ContainerRegistryModel) {
        repositoryLabel.component.text = "${value.address}/"
    }

    override fun resetEditorFrom(configuration: WebAppContainersConfiguration) {
        val state = configuration.state ?: return

        if (state.resourceId.isNullOrEmpty() && state.webAppName.isNullOrEmpty()) return

        val subscription = Subscription(requireNotNull(state.subscriptionId))
        val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
        val resourceGroupName = state.resourceGroupName
        val resourceGroup = ResourceGroupConfig
            .builder()
            .subscriptionId(subscription.id)
            .name(resourceGroupName)
            .region(region)
            .build()
        val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
        val plan = AppServicePlanConfig
            .builder()
            .subscriptionId(subscription.id)
            .name(state.appServicePlanName)
            .resourceGroupName(resourceGroupName)
            .region(region)
            .os(OperatingSystem.LINUX)
            .pricingTier(pricingTier)
            .build()
        val configBuilder = WebAppConfig
            .builder()
            .name(state.webAppName)
            .resourceId(state.resourceId)
            .subscription(subscription)
            .resourceGroup(resourceGroup)
            .servicePlan(plan)
            .runtime(Runtime.DOCKER)
        val webAppConfig =
            if (state.resourceId.isNullOrEmpty()) configBuilder.region(region).pricingTier(pricingTier).build()
            else configBuilder.build()

        webAppContainersComboBox.component.value = webAppConfig

        val imageNameParts = state.imageRepository?.let {
            val parts = it.split('/', limit = 2)
            if (parts.count() == 2) parts[0] to parts[1] else null
        }
        if (imageNameParts != null) {
            containerRegistryComboBox.component.setRegistry(imageNameParts.first)
            repositoryTextField.component.text = imageNameParts.second
        }
        tagTextField.component.text = state.imageTag

        portSpinner.component.number = state.port
    }

    override fun applyEditorTo(configuration: WebAppContainersConfiguration) {
        val state = configuration.state ?: return

        val webappConfig = webAppContainersComboBox.component.value
        val registry = containerRegistryComboBox.component.value
        val repository = repositoryTextField.component.text
        val tag = tagTextField.component.text
        val portValue = portSpinner.component.number

        state.apply {
            resourceId = webappConfig?.resourceId
            webAppName = webappConfig?.name
            subscriptionId = webappConfig?.subscriptionId
            resourceGroupName = webappConfig?.resourceGroup?.name
            region = webappConfig?.region?.toString()
            appServicePlanName = webappConfig?.servicePlan?.name
            appServicePlanResourceGroupName = webappConfig?.servicePlan?.resourceGroupName
            pricingTier = webappConfig?.servicePlan?.pricingTier?.tier
            pricingSize = webappConfig?.servicePlan?.pricingTier?.size
            imageRepository = "${registry?.address}/$repository"
            imageTag = tag
            port = portValue
        }
    }

    override fun createEditor() = panel
}