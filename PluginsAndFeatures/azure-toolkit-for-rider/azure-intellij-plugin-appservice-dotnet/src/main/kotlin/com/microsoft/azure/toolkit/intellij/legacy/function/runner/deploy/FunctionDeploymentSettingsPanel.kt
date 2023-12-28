/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig
import com.microsoft.azure.toolkit.intellij.common.*
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTableUtils
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppComboBox
import com.microsoft.azure.toolkit.intellij.legacy.function.functionAppComboBox
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components.DeploymentSlotComboBox
import com.microsoft.azure.toolkit.intellij.legacy.canBePublishedToAzure
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import java.util.*
import javax.swing.JPanel

class FunctionDeploymentSettingsPanel(private val project: Project, configuration: FunctionDeploymentConfiguration) :
    RiderAzureSettingPanel<FunctionDeploymentConfiguration>() {

    private val panel: JPanel
    private var appSettingsKey: String = configuration.appSettingsKey ?: UUID.randomUUID().toString()
    private var appSettingsResourceId: String? = null
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
                deploymentSlotComboBox = cell(DeploymentSlotComboBox(project))
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
            isRequired = true
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

        deployToSlotCheckBox.component.isEnabled = !value.resourceId.isNullOrEmpty()
        if (value.resourceId.isNullOrEmpty()) deployToSlotCheckBox.component.isSelected = false

        toggleDeploymentSlot(deployToSlotCheckBox.component.isSelected)
        deploymentSlotComboBox.component.setAppService(value.resourceId)

        if (!deployToSlotCheckBox.component.isSelected) {
            loadAppSettings(getResourceId(value, null), value.resourceId.isNullOrEmpty())
        }
    }

    private fun onSelectFunctionSlot(value: DeploymentSlotConfig?) {
        if (value == null) return

        toggleDeploymentSlot(deployToSlotCheckBox.component.isSelected)

        val functionAppConfig = functionAppComboBox.component.value
        if (deployToSlotCheckBox.component.isSelected && functionAppConfig != null) {
            loadAppSettings(getResourceId(functionAppConfig, value), value.isNewCreate)
        }
    }

    private fun onSlotCheckBoxChanged() {
        toggleDeploymentSlot(deployToSlotCheckBox.component.isSelected)

        val function = functionAppComboBox.component.value
        val slot = deploymentSlotComboBox.component.value

        if (deployToSlotCheckBox.component.isSelected && function != null && slot != null) {
            loadAppSettings(getResourceId(function, slot), slot.isNewCreate)
        } else if (!deployToSlotCheckBox.component.isSelected && function != null) {
            loadAppSettings(getResourceId(function, null), function.resourceId.isNullOrEmpty())
        }
    }

    private fun toggleDeploymentSlot(isDeployToSlot: Boolean) {
        deploymentSlotComboBox.component.isEnabled = isDeployToSlot
        deploymentSlotComboBox.component.isRequired = isDeployToSlot
        deploymentSlotComboBox.component.validateValueAsync()
    }

    private fun getResourceId(config: FunctionAppConfig, slotConfig: DeploymentSlotConfig?): String? =
        if (slotConfig == null) {
            if (!config.resourceId.isNullOrEmpty()) config.resourceId
            else Azure.az(AzureFunctions::class.java)
                .functionApps(config.subscriptionId)
                .getOrTemp(config.name, config.resourceGroupName)
                .id
        } else {
            Azure.az(AzureFunctions::class.java)
                .functionApp(config.resourceId)
                ?.slots()
                ?.getOrTemp(slotConfig.name, null)
                ?.id
        }

    private fun loadAppSettings(resourceId: String?, isNewResource: Boolean) {
        if (resourceId?.equals(appSettingsResourceId, true) != false && appSettingsTable.appSettings.isNotEmpty()) {
            return
        }

        appSettingsResourceId = resourceId
        appSettingsTable.loadAppSettings {
            val resource = if (resourceId.isNullOrEmpty() || isNewResource) null else Azure.az().getById(resourceId)
            if (resource is AppServiceAppBase<*, *, *>) resource.appSettings else emptyMap()
        }
    }

    override fun apply(configuration: FunctionDeploymentConfiguration) {
        configuration.appSettingsKey = appSettingsKey

        val functionConfig = functionAppComboBox.component.value
        functionConfig?.let {
            val builder = it.toBuilder()

            if (deployToSlotCheckBox.component.isSelected) builder.deploymentSlot(deploymentSlotComboBox.component.value)
            else builder.deploymentSlot(null)

            builder.appSettings(appSettingsTable.appSettings)

            configuration.functionAppConfig = it
        }

        val publishableProject = dotnetProjectComboBox.component.value
        configuration.publishableProjectPath = publishableProject?.projectFilePath
        val (projectConfiguration, projectPlatform) = configurationAndPlatformComboBox.component.component.getPublishConfiguration()
        configuration.projectConfiguration = projectConfiguration
        configuration.projectPlatform = projectPlatform
    }

    override fun reset(configuration: FunctionDeploymentConfiguration) {
        if (configuration.resourceId.isNullOrEmpty() || configuration.functionAppName.isNullOrEmpty()) return

        val settingsKey = configuration.appSettingsKey
        if (!settingsKey.isNullOrEmpty()) appSettingsKey = settingsKey

        val functionConfig = configuration.functionAppConfig
        functionConfig.let { functionApp ->
            functionAppComboBox.component.value = functionApp
            functionAppComboBox.component.setConfigModel(functionApp)

            deployToSlotCheckBox.component.isSelected = functionApp.deploymentSlot != null
            toggleDeploymentSlot(functionApp.deploymentSlot != null)
            functionApp.deploymentSlot?.let { deploymentSlotComboBox.component.value = it }

            appSettingsResourceId =
                if (functionApp.resourceId.isNullOrEmpty() && functionApp.name.isNullOrEmpty()) null
                else getResourceId(functionApp, functionApp.deploymentSlot)

            functionApp.appSettings?.let { appSettingsTable.setAppSettings(it) }
        }

        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { p -> p.projectFilePath == configuration.publishableProjectPath }
        if (publishableProject != null) {
            dotnetProjectComboBox.component.setProject(publishableProject)
        }
        configurationAndPlatformComboBox.component.component.setPublishConfiguration(
            configuration.projectConfiguration,
            configuration.projectPlatform
        )
    }

    override fun getMainPanel() = panel

    override fun disposeEditor() {
    }
}