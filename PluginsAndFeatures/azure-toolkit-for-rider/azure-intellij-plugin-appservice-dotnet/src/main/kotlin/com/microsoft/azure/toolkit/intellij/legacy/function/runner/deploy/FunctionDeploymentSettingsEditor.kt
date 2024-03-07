/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.selectedValueMatches
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig
import com.microsoft.azure.toolkit.intellij.common.*
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTableUtils
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components.DeploymentSlotComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import javax.swing.JPanel

class FunctionDeploymentSettingsEditor(private val project: Project) :
    SettingsEditor<FunctionDeploymentConfiguration>() {

    private val panel: JPanel
    private lateinit var functionAppComboBox: Cell<FunctionAppComboBox>
    private lateinit var deployToSlotCheckBox: Cell<JBCheckBox>
    private lateinit var deploymentSlotComboBox: Cell<DeploymentSlotComboBox>
    private lateinit var dotnetProjectComboBox: Cell<AzureDotnetProjectComboBox>
    private lateinit var configurationAndPlatformComboBox: Cell<LabeledComponent<ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>>>
    private lateinit var appSettingsTable: AppSettingsTable

    init {
        panel = panel {
            row("Function:") {
                functionAppComboBox = functionAppComboBox(project)
                    .align(Align.FILL)
                    .resizableColumn()
                Disposer.register(this@FunctionDeploymentSettingsEditor, functionAppComboBox.component)
            }
            row {
                deployToSlotCheckBox = checkBox("Deploy to Slot:")
                    .enabledIf(functionAppComboBox.component.selectedValueMatches {
                        it?.resourceId.isNullOrEmpty().not()
                    })
                deploymentSlotComboBox = cell(DeploymentSlotComboBox(project))
                    .enabledIf(deployToSlotCheckBox.selected)
                    .align(Align.FILL)
            }
            row("Project:") {
                dotnetProjectComboBox = dotnetProjectComboBox(project) { it.isAzureFunction }
                    .align(Align.FILL)
            }
            row("Configuration:") {
                configurationAndPlatformComboBox = configurationAndPlatformComboBox(project)
                    .align(Align.FILL)
            }
            row("App Settings:") {
                appSettingsTable = AppSettingsTable()
                cell(AppSettingsTableUtils.createAppSettingPanel(appSettingsTable))
                    .align(Align.FILL)
            }
        }

        functionAppComboBox.component.apply {
            addValueChangedListener(::onSelectFunctionApp)
            reloadItems()
        }
        deploymentSlotComboBox.component.apply {
            addValueChangedListener(::onSelectFunctionSlot)
            reloadItems()
        }
        deployToSlotCheckBox.component.addItemListener { onSlotCheckBoxChanged() }
        dotnetProjectComboBox.component.reloadItems()
    }

    private fun onSelectFunctionApp(value: FunctionAppConfig?) {
        if (value == null) return

        if (value.resourceId.isNullOrEmpty()) {
            deployToSlotCheckBox.component.isSelected = false
        }

        deploymentSlotComboBox.component.setAppService(value.resourceId)

        if (!deployToSlotCheckBox.component.isSelected) {
            loadFunctionAppSettings(value)
        }
    }

    private fun onSelectFunctionSlot(value: DeploymentSlotConfig?) {
        if (value == null) return

        val functionAppConfig = functionAppComboBox.component.value
        if (deployToSlotCheckBox.component.isSelected && functionAppConfig != null) {
            loadFunctionSlotSettings(functionAppConfig, value)
        }
    }

    private fun onSlotCheckBoxChanged() {
        val functionAppConfig = functionAppComboBox.component.value
        val slotConfig = deploymentSlotComboBox.component.value

        if (deployToSlotCheckBox.component.isSelected && functionAppConfig != null && slotConfig != null) {
            loadFunctionSlotSettings(functionAppConfig, slotConfig)
        } else if (!deployToSlotCheckBox.component.isSelected && functionAppConfig != null) {
            loadFunctionAppSettings(functionAppConfig)
        }
    }

    private fun loadFunctionAppSettings(functionAppConfig: FunctionAppConfig) {
        val resourceId = functionAppConfig.resourceId
        if (resourceId != null) {
            appSettingsTable.loadAppSettings {
                Azure.az(AzureFunctions::class.java).functionApp(resourceId)?.appSettings
            }
        } else {
            appSettingsTable.loadAppSettings {
                functionAppConfig.appSettings
            }
        }
    }

    private fun loadFunctionSlotSettings(
        functionAppConfig: FunctionAppConfig,
        deploymentSlotConfig: DeploymentSlotConfig
    ) {
        if (deploymentSlotConfig.isNewCreate) {
            val resourceId = functionAppConfig.resourceId
            if (resourceId != null) {
                appSettingsTable.loadAppSettings {
                    Azure.az(AzureFunctions::class.java).functionApp(resourceId)?.appSettings
                }
            } else {
                appSettingsTable.loadAppSettings {
                    functionAppConfig.appSettings
                }
            }
        } else {
            appSettingsTable.loadAppSettings {
                Azure.az(AzureFunctions::class.java)
                    .functionApp(functionAppConfig.resourceId)
                    ?.slots()
                    ?.get(deploymentSlotConfig.name, null)
                    ?.appSettings
            }
        }
    }

    override fun resetEditorFrom(configuration: FunctionDeploymentConfiguration) {
        val state = configuration.state ?: return

        if (state.resourceId.isNullOrEmpty() && state.functionAppName.isNullOrEmpty()) return

        val subscription = Subscription(requireNotNull(state.subscriptionId))
        val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
        val resourceGroup = ResourceGroupConfig
            .builder()
            .subscriptionId(subscription.id)
            .name(state.resourceGroupName)
            .region(region)
            .build()
        val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
        val operatingSystem = OperatingSystem.fromString(state.operatingSystem)
        val plan = AppServicePlanConfig
            .builder()
            .subscriptionId(subscription.id)
            .name(state.appServicePlanName)
            .resourceGroupName(state.appServicePlanResourceGroupName)
            .region(region)
            .os(operatingSystem)
            .pricingTier(pricingTier)
            .build()
        val slotConfig = if (state.isDeployToSlot) {
            if (state.slotName.isNullOrEmpty())
                DeploymentSlotConfig
                    .builder()
                    .newCreate(true)
                    .name(state.newSlotName)
                    .configurationSource(state.newSlotConfigurationSource)
                    .build()
            else
                DeploymentSlotConfig
                    .builder()
                    .newCreate(false)
                    .name(state.slotName)
                    .build()
        } else null
        val configBuilder = FunctionAppConfig
            .builder()
            .name(state.functionAppName)
            .resourceId(state.resourceId)
            .subscription(subscription)
            .resourceGroup(resourceGroup)
            .servicePlan(plan)
            .runtime(if (operatingSystem == OperatingSystem.LINUX) FunctionAppLinuxRuntime.FUNCTION_JAVA17 else FunctionAppWindowsRuntime.FUNCTION_JAVA17)
            .deploymentSlot(slotConfig)
            .appSettings(state.appSettings)
        val functionAppConfig =
            if (state.resourceId.isNullOrEmpty()) configBuilder.region(region).pricingTier(pricingTier).build()
            else configBuilder.build()

        functionAppComboBox.component.value = functionAppConfig
        if (state.isDeployToSlot) {
            deployToSlotCheckBox.selected(true)
            deploymentSlotComboBox.component.value = slotConfig
        } else {
            deployToSlotCheckBox.selected(false)
            deploymentSlotComboBox.component.clear()
        }
        appSettingsTable.setAppSettings(state.appSettings)

        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { p -> p.projectFilePath == state.publishableProjectPath }
        if (publishableProject != null) {
            dotnetProjectComboBox.component.setProject(publishableProject)
        }
        configurationAndPlatformComboBox.component.component.setPublishConfiguration(
            state.projectConfiguration ?: "",
            state.projectPlatform ?: ""
        )
    }

    override fun applyEditorTo(configuration: FunctionDeploymentConfiguration) {
        val state = configuration.state ?: return

        val functionConfig = functionAppComboBox.component.value
        val deployToSlot = deployToSlotCheckBox.component.isSelected
        val slotConfig = deploymentSlotComboBox.component.value

        state.apply {
            resourceId = functionConfig?.resourceId
            functionAppName = functionConfig?.name
            subscriptionId = functionConfig?.subscriptionId
            resourceGroupName = functionConfig?.resourceGroup?.name
            region = functionConfig?.region?.toString()
            appServicePlanName = functionConfig?.servicePlan?.name
            appServicePlanResourceGroupName = functionConfig?.servicePlan?.resourceGroupName
            pricingTier = functionConfig?.servicePlan?.pricingTier?.tier
            pricingSize = functionConfig?.servicePlan?.pricingTier?.size
            operatingSystem = functionConfig?.runtime?.operatingSystem?.toString()
            isDeployToSlot = deployToSlot
            if (!deployToSlot || slotConfig == null) {
                slotName = null
                newSlotName = null
                newSlotConfigurationSource = null
            } else {
                if (slotConfig.isNewCreate) {
                    slotName = null
                    newSlotName = slotConfig.name
                    newSlotConfigurationSource = slotConfig.configurationSource
                } else {
                    slotName = slotConfig.name
                    newSlotName = null
                    newSlotConfigurationSource = null
                }
            }
            storageAccountName = null
            storageAccountResourceGroup = null
            appSettings = appSettingsTable.appSettings
            publishableProjectPath = dotnetProjectComboBox.component.value?.projectFilePath
            val (config, platform) = configurationAndPlatformComboBox.component.component.getPublishConfiguration()
            projectConfiguration = config
            projectPlatform = platform
        }
    }

    override fun createEditor() = panel
}