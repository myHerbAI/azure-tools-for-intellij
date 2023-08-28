package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox
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

class RiderAppServiceInfoAdvancedPanel<T>(
        project: Project,
        private val defaultConfigSupplier: Supplier<T>
) : JPanel(), AzureFormPanel<T> where T : AppServiceConfig {
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

    private val selectorApplication = AzureDotnetProjectComboBox(project) { _ -> true }

    private var operatingSystem: OperatingSystem
    private lateinit var windowsRadioButton: Cell<JBRadioButton>
    private lateinit var linuxRadioButton: Cell<JBRadioButton>

    private lateinit var deploymentGroup: Row

    init {
        operatingSystem = OperatingSystem.WINDOWS

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
                buttonsGroup {
                    row("Operating System:") {
                        windowsRadioButton = radioButton("Windows", OperatingSystem.WINDOWS)
                        windowsRadioButton.component.addItemListener { onOperatingSystemChanged(it) }
                        linuxRadioButton = radioButton("Linux", OperatingSystem.LINUX)
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
                    cell(selectorApplication)
                            .align(Align.FILL)
                }
            }
        }

        add(panel)
    }

    override fun getValue(): T {
        val subscription = selectorSubscription.value
        val resourceGroup = selectorGroup.value
        val name = textName.value
        val os = if (windowsRadioButton.component.isSelected) OperatingSystem.WINDOWS else OperatingSystem.LINUX
        val region = selectorRegion.value
        val servicePlan = selectorServicePlan.value
        val project = selectorApplication.value

        val config = defaultConfigSupplier.get()
        config.subscription = subscription
        config.resourceGroup = ResourceGroupConfig.fromResource(resourceGroup)
        config.name = name
        config.runtime = Runtime(os, WebContainer("DOTNETCORE 7.0"), null)
        config.region = region
        val planConfig = AppServicePlanConfig.fromResource(servicePlan)
        if (planConfig != null && servicePlan?.isDraftForCreating == true) {
            planConfig.resourceGroupName = config.resourceGroupName
            planConfig.region = region
            planConfig.os = os
        }
        config.servicePlan = planConfig
        if (project != null) {
            config.application = Path(project.projectFilePath)
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
            selectorApplication,
            selectorServicePlan
    )

    override fun setVisible(visible: Boolean) {
        panel.isVisible = visible
        super<JPanel>.setVisible(visible)
    }

    fun setDeploymentVisible(visible: Boolean) {
        deploymentGroup.visible(visible)
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
}