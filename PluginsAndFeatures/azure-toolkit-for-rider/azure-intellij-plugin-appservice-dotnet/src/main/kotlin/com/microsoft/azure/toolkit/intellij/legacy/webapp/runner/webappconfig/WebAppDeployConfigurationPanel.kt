/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.and
import com.intellij.ui.layout.selectedValueMatches
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.ide.appservice.model.AzureArtifactConfig
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppDeployRunConfigurationModel
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.component.UIUtils
import com.microsoft.azure.toolkit.intellij.common.configurationAndPlatformComboBox
import com.microsoft.azure.toolkit.intellij.common.dotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTableUtils
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_CONFIGURATION
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_PLATFORM
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput.AzureValueChangeBiListener
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import org.apache.commons.collections4.MapUtils
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JComboBox
import javax.swing.JPanel

class WebAppDeployConfigurationPanel(private val project: Project) : AzureFormPanel<WebAppDeployRunConfigurationModel> {
    companion object {
        private const val DEFAULT_SLOT_NAME = "slot"
    }

    private val formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss")

    val panel: JPanel

    private lateinit var webAppComboBox: Cell<WebAppComboBox>
    private lateinit var dotnetProjectComboBox: Cell<AzureDotnetProjectComboBox>
    private lateinit var configurationAndPlatformComboBox: Cell<LabeledComponent<ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>>>
    private lateinit var appSettingsTable: AppSettingsTable
    private lateinit var deploymentSlotGroup: CollapsibleRow
    private lateinit var deployToSlotCheckBox: Cell<JBCheckBox>
    private lateinit var newSlotRadioButton: Cell<JBRadioButton>
    private lateinit var existingSlotRadioButton: Cell<JBRadioButton>
    private lateinit var newSlotTextField: Cell<JBTextField>
    private lateinit var existingSlotNameComboBox: Cell<ComboBox<String>>
    private lateinit var slotConfigurationSourceComboBox: Cell<ComboBox<String>>
    private lateinit var noAvailableSlotLink: Cell<ActionLink>
    private lateinit var openBrowserCheckBox: Cell<JBCheckBox>

    private val lock = Any()

    init {
        panel = panel {
            row("Web App:") {
                webAppComboBox = webAppComboBox(project)
                    .align(Align.FILL)
                    .resizableColumn()
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
            deploymentSlotGroup = collapsibleGroup("Deployment Slot") {
                row {
                    deployToSlotCheckBox = checkBox("Deploy to slot")
                }
                buttonsGroup {
                    row {
                        newSlotRadioButton = radioButton("Create new slot")
                            .enabledIf(deployToSlotCheckBox.selected.and(webAppComboBox.component.selectedValueMatches { it != null }))
                        existingSlotRadioButton = radioButton("Use existing slot")
                            .enabledIf(deployToSlotCheckBox.selected.and(webAppComboBox.component.selectedValueMatches { it != null }))
                    }
                }
                row("Slot Name:") {
                    newSlotTextField = textField()
                        .enabledIf(deployToSlotCheckBox.selected.and(webAppComboBox.component.selectedValueMatches { it != null }))
                        .enabledIf(newSlotRadioButton.selected)
                        .visibleIf(newSlotRadioButton.selected)
                        .align(Align.FILL)

                    existingSlotNameComboBox = comboBox(emptyList<String>())
                        .enabledIf(deployToSlotCheckBox.selected.and(webAppComboBox.component.selectedValueMatches { it != null }))
                        .enabledIf(existingSlotRadioButton.selected)
                        .visibleIf(existingSlotRadioButton.selected)
                        .align(Align.FILL)
                }
                row {
                    noAvailableSlotLink = link("No available deployment slot, click to create a new one") {
                        newSlotRadioButton.component.doClick()
                    }
                        .visibleIf(existingSlotRadioButton.selected.and(existingSlotNameComboBox.component.selectedValueMatches { it == null }))
                }
                row("Slot Configuration Source:") {
                    slotConfigurationSourceComboBox = comboBox(emptyList<String>())
                        .enabledIf(deployToSlotCheckBox.selected.and(webAppComboBox.component.selectedValueMatches { it != null }))
                        .enabledIf(newSlotRadioButton.selected)
                        .align(Align.FILL)
                }
                    .enabledIf(newSlotRadioButton.selected)
                    .visibleIf(newSlotRadioButton.selected)
            }
            row {
                openBrowserCheckBox = checkBox("Open browser after deployment")
            }
        }

        webAppComboBox.component.apply {
            addValueChangedListener(AzureValueChangeBiListener(::onWebAppChanged))
            reloadItems()
        }
        dotnetProjectComboBox.component.reloadItems()
        newSlotRadioButton.component.isSelected = true
        newSlotTextField.component.text = "${DEFAULT_SLOT_NAME}-${LocalDateTime.now().format(formatter)}"
    }

    override fun setVisible(visible: Boolean) {
    }

    private fun onWebAppChanged(value: WebAppConfig, before: WebAppConfig?) {
        loadDeploymentSlot(value)
        loadAppSettings(value, before)
    }

    private fun loadDeploymentSlot(selectedWebApp: WebAppConfig) {
        val resourceId = selectedWebApp.resourceId
        if (resourceId.isNullOrEmpty()) {
            deployToSlotCheckBox.component.isEnabled = false
            deployToSlotCheckBox.component.isSelected = false
        } else {
            deployToSlotCheckBox.component.isEnabled = true
            Mono.fromCallable { Azure.az(AzureWebApp::class.java).webApp(resourceId) }
                .map { it?.slots()?.list() ?: emptyList() }
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe {
                    AzureTaskManager.getInstance()
                        .runLater({ fillDeploymentSlots(it, selectedWebApp) }, AzureTask.Modality.ANY)
                }
        }
    }

    private fun fillDeploymentSlots(slotList: List<WebAppDeploymentSlot>, selectedWebApp: WebAppConfig) {
        synchronized(lock) {
            val defaultSlot = existingSlotNameComboBox.component.selectedItem as? String?
            val defaultConfigurationSource = slotConfigurationSourceComboBox.component.selectedItem as? String?
            existingSlotNameComboBox.component.removeAllItems()
            slotConfigurationSourceComboBox.component.removeAllItems()
            slotConfigurationSourceComboBox.component.addItem("Don't clone configuration from an existing slot")
            slotConfigurationSourceComboBox.component.addItem(selectedWebApp.name)
            slotList.stream().filter { it != null }.forEach {
                existingSlotNameComboBox.component.addItem(it.name)
                slotConfigurationSourceComboBox.component.addItem(it.name)
            }
            setComboBoxDefaultValue(existingSlotNameComboBox.component, defaultSlot)
            setComboBoxDefaultValue(slotConfigurationSourceComboBox.component, defaultConfigurationSource)
        }
    }

    private fun loadAppSettings(selectedWebApp: WebAppConfig, before: WebAppConfig?) {
        val rawValue =
            if (webAppComboBox.component.rawValue is WebAppConfig) webAppComboBox.component.rawValue as WebAppConfig
            else selectedWebApp
        if (before == null && selectedWebApp != rawValue) {
            if (rawValue.resourceId.isNullOrEmpty() && !selectedWebApp.resourceId.isNullOrEmpty()) {
                appSettingsTable.loadAppSettings { loadDraftAppSettings(rawValue) }
            }
        } else if (selectedWebApp != before) {
            appSettingsTable.loadAppSettings {
                if (selectedWebApp.resourceId.isNullOrEmpty()) {
                    selectedWebApp.appSettings
                } else {
                    Azure.az(AzureWebApp::class.java).webApp(selectedWebApp.resourceId)?.appSettings
                }
            }
        }
    }

    private fun loadDraftAppSettings(value: WebAppConfig): Map<String, String> {
        val webApp =
            Azure.az(AzureWebApp::class.java).webApps(value.subscriptionId).get(value.name, value.resourceGroupName)
        return if (webApp != null && webApp.exists())
            MapUtils.putAll(webApp.appSettings, value.appSettings.entries.toTypedArray())
        else
            value.appSettings
    }

    override fun setValue(data: WebAppDeployRunConfigurationModel) {
        data.artifactConfig?.let {
            val artifactId = it.artifactIdentifier?.toIntOrNull()
            if (artifactId != null) {
                project.solution.publishableProjectsModel.publishableProjects.values
                    .firstOrNull { p -> p.projectModelId == artifactId }
                    ?.let { p -> dotnetProjectComboBox.component.setProject(p) }
            }
        }

        data.webAppConfig?.let { webApp ->
            webAppComboBox.component.setConfigModel(webApp)
            webAppComboBox.component.value = webApp
            appSettingsTable.setAppSettings(webApp.appSettings)

            webApp.deploymentSlot?.let { slot ->
                deployToSlotCheckBox.component.isSelected = true
                existingSlotRadioButton.component.isSelected = !slot.isNewCreate
                if (slot.isNewCreate) {
                    newSlotTextField.component.text = slot.name
                    slotConfigurationSourceComboBox.component.addItem(slot.configurationSource)
                    slotConfigurationSourceComboBox.component.selectedItem = slot.configurationSource
                } else {
                    existingSlotNameComboBox.component.addItem(slot.name)
                    existingSlotNameComboBox.component.selectedItem = slot.name
                }
            }
        }

        openBrowserCheckBox.component.isSelected = data.isOpenBrowserAfterDeployment
        deploymentSlotGroup.expanded = data.isSlotPanelVisible
    }

    fun setConfigurationAndPlatform(configuration: String, platform: String) {
        for (i in 0 until configurationAndPlatformComboBox.component.component.model.size) {
            val item = configurationAndPlatformComboBox.component.component.model.getElementAt(i)
            if (item?.configuration == configuration && item.platform == platform) {
                configurationAndPlatformComboBox.component.component.selectedItem = item
                break
            }
        }
    }

    override fun getValue(): WebAppDeployRunConfigurationModel {
        val project = dotnetProjectComboBox.component.value
        val artifactConfig = project?.let {
            AzureArtifactConfig.builder().artifactIdentifier(it.projectModelId.toString()).build()
        }

        val slotConfig =
            if (deployToSlotCheckBox.component.isSelected) {
                if (existingSlotRadioButton.component.isSelected) {
                    DeploymentSlotConfig
                        .builder()
                        .newCreate(false)
                        .name(existingSlotNameComboBox.component.selectedItem as? String?)
                        .build()
                } else {
                    DeploymentSlotConfig
                        .builder()
                        .newCreate(true)
                        .name(newSlotTextField.component.text)
                        .configurationSource(slotConfigurationSourceComboBox.component.selectedItem as? String?)
                        .build()
                }
            } else null

        if (webAppComboBox.component.value?.appSettings?.containsKey(RIDER_PROJECT_CONFIGURATION) == true)
            webAppComboBox.component.value?.appSettings?.remove(RIDER_PROJECT_CONFIGURATION)
        if (webAppComboBox.component.value?.appSettings?.containsKey(RIDER_PROJECT_PLATFORM) == true)
            webAppComboBox.component.value?.appSettings?.remove(RIDER_PROJECT_PLATFORM)

        val webAppConfig = webAppComboBox.component.value
            ?.toBuilder()
            ?.appSettings(appSettingsTable.appSettings)
            ?.deploymentSlot(slotConfig)
            ?.build()

        return WebAppDeployRunConfigurationModel
            .builder()
            .webAppConfig(webAppConfig)
            .artifactConfig(artifactConfig)
            .openBrowserAfterDeployment(openBrowserCheckBox.component.isSelected)
            .slotPanelVisible(deploymentSlotGroup.expanded)
            .build()
    }

    fun getSelectedConfiguration() = getSelectedConfigurationAndPlatform()?.configuration ?: ""
    fun getSelectedPlatform() = getSelectedConfigurationAndPlatform()?.platform ?: ""
    private fun getSelectedConfigurationAndPlatform(): PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform? =
        configurationAndPlatformComboBox.component.component.selectedItem as? PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform

    override fun getInputs() = listOf(webAppComboBox.component, dotnetProjectComboBox.component)

    private fun setComboBoxDefaultValue(comboBox: JComboBox<*>, value: Any?) {
        UIUtils.listComboBoxItems(comboBox).stream().filter { it == value }.findFirst()
            .ifPresent { comboBox.selectedItem = value }
    }
}