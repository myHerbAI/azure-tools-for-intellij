/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
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
import com.microsoft.azure.toolkit.intellij.legacy.canBePublishedToAzure
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppComboBox
import com.microsoft.azure.toolkit.intellij.legacy.function.functionAppComboBox
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components.DeploymentSlotComboBox
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.model.*
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
                dotnetProjectComboBox = dotnetProjectComboBox(project) { it.canBePublishedToAzure() }
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
//            loadAppSettings(getResourceId(functionAppConfig, value), value.isNewCreate)
        }
    }

    private fun onSlotCheckBoxChanged() {
        val functionAppConfig = functionAppComboBox.component.value
        val slotConfig = deploymentSlotComboBox.component.value

        if (deployToSlotCheckBox.component.isSelected && functionAppConfig != null && slotConfig != null) {
//            loadAppSettings(getResourceId(functionAppConfig, slotConfig), slotConfig.isNewCreate)
        } else if (!deployToSlotCheckBox.component.isSelected && functionAppConfig != null) {
//            loadAppSettings(getResourceId(functionAppConfig, null), functionAppConfig.resourceId.isNullOrEmpty())
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

    override fun resetEditorFrom(configuration: FunctionDeploymentConfiguration) {
        val state = configuration.state ?: return

        if (state.resourceId.isNullOrEmpty() && state.functionAppName.isNullOrEmpty()) return

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
        val operatingSystem = OperatingSystem.fromString(state.operatingSystem)
        val plan = AppServicePlanConfig
            .builder()
            .subscriptionId(subscription.id)
            .name(state.appServicePlanName)
            .resourceGroupName(resourceGroupName)
            .region(region)
            .os(operatingSystem)
            .pricingTier(pricingTier)
            .build()
//        val slotConfig = if (configuration.isDeployToSlot) {
//            if (configuration.slotName.isNullOrEmpty())
//                DeploymentSlotConfig
//                    .builder()
//                    .newCreate(true)
//                    .name(configuration.newSlotName)
//                    .configurationSource(configuration.newSlotConfigurationSource)
//                    .build()
//            else
//                DeploymentSlotConfig
//                    .builder()
//                    .newCreate(false)
//                    .name(configuration.slotName)
//                    .build()
//        } else null
        val configBuilder = FunctionAppConfig
            .builder()
            .name(state.functionAppName)
            .resourceId(state.resourceId)
            .subscription(subscription)
            .resourceGroup(resourceGroup)
            .servicePlan(plan)
            .runtime(Runtime(operatingSystem, WebContainer.JAVA_OFF, JavaVersion.OFF))
            //.deploymentSlot(slotConfig)
            .appSettings(state.appSettings)
        val functionAppConfig =
            if (state.resourceId.isNullOrEmpty()) configBuilder.region(region).pricingTier(pricingTier).build()
            else configBuilder.build()
        functionAppComboBox.component.value = functionAppConfig

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
//        if (configuration.resourceId.isNullOrEmpty() && configuration.functionAppName.isNullOrEmpty()) return
//
//        val settingsKey = configuration.appSettingsKey
//        if (!settingsKey.isNullOrEmpty()) appSettingsKey = settingsKey
//
//        val functionConfig = configuration.functionAppConfig
//        functionConfig.let { functionApp ->
//            functionAppComboBox.component.value = functionApp
//            functionAppComboBox.component.setConfigModel(functionApp)
//
//            deployToSlotCheckBox.component.isSelected = functionApp.deploymentSlot != null
//            toggleDeploymentSlot(functionApp.deploymentSlot != null)
//            functionApp.deploymentSlot?.let { deploymentSlotComboBox.component.value = it }
//
//            appSettingsResourceId =
//                if (functionApp.resourceId.isNullOrEmpty() && functionApp.name.isNullOrEmpty()) null
//                else getResourceId(functionApp, functionApp.deploymentSlot)
//
//            functionApp.appSettings?.let { appSettingsTable.setAppSettings(it) }
//        }
    }

    override fun applyEditorTo(configuration: FunctionDeploymentConfiguration) {
        val state = configuration.state ?: return

        val functionConfig = functionAppComboBox.component.value
        if (functionConfig?.appSettings?.containsKey(WebAppCreationDialog.RIDER_PROJECT_CONFIGURATION) == true)
            functionConfig.appSettings?.remove(WebAppCreationDialog.RIDER_PROJECT_CONFIGURATION)
        if (functionConfig?.appSettings?.containsKey(WebAppCreationDialog.RIDER_PROJECT_PLATFORM) == true)
            functionConfig.appSettings?.remove(WebAppCreationDialog.RIDER_PROJECT_PLATFORM)

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