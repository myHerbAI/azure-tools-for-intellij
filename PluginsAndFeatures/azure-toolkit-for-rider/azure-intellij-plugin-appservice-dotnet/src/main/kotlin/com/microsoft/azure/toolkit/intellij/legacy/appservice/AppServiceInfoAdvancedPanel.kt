/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox
import com.microsoft.azure.toolkit.intellij.common.configurationAndPlatformComboBox
import com.microsoft.azure.toolkit.intellij.common.dotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox
import com.microsoft.azure.toolkit.intellij.legacy.canBePublishedToAzure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.model.*
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import java.awt.event.ItemEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Supplier
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.io.path.Path

class AppServiceInfoAdvancedPanel<T>(
        private val project: Project,
        private val defaultConfigSupplier: Supplier<T>
) : JPanel(), Disposable, AzureFormPanel<T> where T : AppServiceConfig {
    companion object {
        private const val NOT_APPLICABLE = "N/A"
    }

    private val formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss")

    private val panel: JPanel
    private val textName = AppNameInput().apply {
        isRequired = true
        value = "app-${project.name}-${LocalDateTime.now().format(formatter)}"
    }
    private val selectorSubscription = SubscriptionComboBox().apply {
        isRequired = true
        addItemListener { onSubscriptionChanged(it) }
    }
    private val selectorGroup = ResourceGroupComboBox().apply {
        isRequired = true
        addItemListener { onGroupChanged(it) }
    }
    private val selectorRegion = RegionComboBox().apply {
        isRequired = true
        addItemListener { onRegionChanged(it) }
    }
    private val selectorServicePlan = ServicePlanComboBox().apply {
        isRequired = true
        addItemListener { onServicePlanChanged(it) }
    }
    private val textSku = JLabel().apply {
        text = NOT_APPLICABLE
        border = JBUI.Borders.emptyLeft(5)
    }

    private var operatingSystem: OperatingSystem
    private lateinit var operatingSystemGroup: ButtonsGroup
    private lateinit var windowsRadioButton: Cell<JBRadioButton>
    private lateinit var linuxRadioButton: Cell<JBRadioButton>
    private lateinit var deploymentGroup: Row
    private lateinit var dotnetProjectComboBox: Cell<AzureDotnetProjectComboBox>
    private lateinit var configurationAndPlatformComboBox: Cell<LabeledComponent<ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>>>

    init {
        operatingSystem = OperatingSystem.LINUX

        Disposer.register(this, textName)

        panel = panel {
            group("Project Details") {
                row("Subscription:") {
                    cell(selectorSubscription)
                            .align(Align.FILL)
                }
                row("Resource Group:") {
                    cell(selectorGroup)
                            .align(Align.FILL)
                }
            }
            group("Instance Details") {
                row("Name:") {
                    cell(textName)
                    label(".azurewebsites.net")
                }
                operatingSystemGroup = buttonsGroup {
                    row("Operating System:") {
                        linuxRadioButton = radioButton("Linux", OperatingSystem.LINUX)
                        windowsRadioButton = radioButton("Windows", OperatingSystem.WINDOWS)
                        windowsRadioButton.component.addItemListener { onOperatingSystemChanged(it) }
                    }
                }.bind(::operatingSystem)
                row("Region:") {
                    cell(selectorRegion)
                            .align(Align.FILL)
                }
            }
            group("App Service Plan") {
                row("Plan:") {
                    cell(selectorServicePlan)
                            .align(Align.FILL)
                }
                row("SKU and size:") {
                    cell(textSku)
                }
            }
            deploymentGroup = group("Deployment") {
                row("Project:") {
                    dotnetProjectComboBox = dotnetProjectComboBox(project) { it.canBePublishedToAzure() }
                            .align(Align.FILL)
                }
                row("Configuration:") {
                    configurationAndPlatformComboBox = configurationAndPlatformComboBox(project)
                            .align(Align.FILL)
                }
            }
        }
        dotnetProjectComboBox.component.reloadItems()

        add(panel)
    }

    override fun getValue(): T {
        val subscription = selectorSubscription.value
        val resourceGroup = selectorGroup.value
        val name = textName.value
        val os = if (windowsRadioButton.component.isSelected) OperatingSystem.WINDOWS else OperatingSystem.LINUX
        val region = selectorRegion.value
        val servicePlan = selectorServicePlan.value
        val projectModel = dotnetProjectComboBox.component.value
        val configurationAndPlatform = getSelectedConfigurationAndPlatform()

        val config = defaultConfigSupplier.get()
        config.subscription = subscription
        config.resourceGroup = ResourceGroupConfig.fromResource(resourceGroup)
        config.name = name
        config.runtime = Runtime(os, WebContainer.JAVA_OFF, JavaVersion.OFF)
        config.region = region
        val planConfig = AppServicePlanConfig.fromResource(servicePlan)
        if (planConfig != null && servicePlan?.isDraftForCreating == true) {
            planConfig.resourceGroupName = config.resourceGroupName
            planConfig.region = region
            planConfig.os = os
        }
        config.servicePlan = planConfig

        if (projectModel != null) {
            config.application = Path(projectModel.projectFilePath)
        }
        if (configurationAndPlatform != null) {
            config.appSettings[RIDER_PROJECT_CONFIGURATION] = configurationAndPlatform.configuration
            config.appSettings[RIDER_PROJECT_PLATFORM] = configurationAndPlatform.platform
        }

        return config
    }

    override fun setValue(config: T) {
        selectorSubscription.value = config.subscription
        textName.value = config.name
        if (config.runtime.operatingSystem == OperatingSystem.WINDOWS) {
            windowsRadioButton.component.isSelected = true
        } else {
            linuxRadioButton.component.isSelected = true
        }
        AzureTaskManager.getInstance().runOnPooledThread {
            selectorGroup.value = config.resourceGroup?.toResource()
            selectorServicePlan.value = config.servicePlan?.toResource()
            selectorRegion.value = config.region
        }
    }

    override fun getInputs(): List<AzureFormInput<*>> = listOf(
            textName,
            selectorSubscription,
            selectorGroup,
            selectorRegion,
            selectorServicePlan
    )

    override fun setVisible(visible: Boolean) {
        panel.isVisible = visible
        super<JPanel>.setVisible(visible)
    }

    private fun onServicePlanChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED) {
            val plan = e.item as? AppServicePlan ?: return
            if (plan.pricingTier == null) return
            val pricing = if (plan.pricingTier == PricingTier.CONSUMPTION) "Consumption" else "${plan.pricingTier.tier}_${plan.pricingTier.size}"
            textSku.text = pricing
        } else if (e.stateChange == ItemEvent.DESELECTED) {
            textSku.text = NOT_APPLICABLE
        }
    }

    private fun onSubscriptionChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val subscription = e.item as? Subscription ?: return
            selectorGroup.setSubscription(subscription)
            textName.setSubscription(subscription)
            selectorRegion.setSubscription(subscription)
            selectorServicePlan.setSubscription(subscription)
        }
    }

    private fun onOperatingSystemChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED) {
            selectorServicePlan.setOperatingSystem(OperatingSystem.WINDOWS)
        } else {
            selectorServicePlan.setOperatingSystem(OperatingSystem.LINUX)
        }
    }

    private fun onRegionChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val region = e.item as? Region ?: return
            selectorServicePlan.setRegion(region)
        }
    }

    private fun onGroupChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val resourceGroup = e.item as? ResourceGroup ?: return
            selectorServicePlan.setResourceGroup(resourceGroup)
        }
    }

    fun setValidPricingTier(pricingTier: List<PricingTier>, defaultPricingTier: PricingTier) {
        selectorServicePlan.setValidPricingTierList(pricingTier, defaultPricingTier)
    }

    fun setDeploymentVisible(visible: Boolean) {
        deploymentGroup.visible(visible)
    }

    fun setFixedRuntime(runtime: Runtime) {
        if (runtime.operatingSystem == OperatingSystem.WINDOWS) {
            windowsRadioButton.component.isSelected = true
        } else {
            linuxRadioButton.component.isSelected = true
        }
        operatingSystemGroup.visible(false)
    }

    private fun getSelectedConfigurationAndPlatform(): PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform? =
            configurationAndPlatformComboBox.component.component.selectedItem as? PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform

    override fun dispose() {
    }
}