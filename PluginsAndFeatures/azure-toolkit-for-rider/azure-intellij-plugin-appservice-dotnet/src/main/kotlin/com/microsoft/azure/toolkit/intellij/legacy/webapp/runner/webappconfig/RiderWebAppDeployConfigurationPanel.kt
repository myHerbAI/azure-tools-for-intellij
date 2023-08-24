package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.and
import com.intellij.ui.layout.selected
import com.intellij.ui.layout.selectedValueMatches
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.ide.appservice.model.AzureArtifactConfig
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppDeployRunConfigurationModel
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.dotnetProjectComboBox
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput.AzureValueChangeBiListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JPanel

class RiderWebAppDeployConfigurationPanel(private val project: Project) : AzureFormPanel<WebAppDeployRunConfigurationModel> {
    companion object {
        private const val DEFAULT_SLOT_NAME = "slot"
    }

    private val formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss")

    val panel: JPanel

    private lateinit var webAppComboBox: Cell<WebAppComboBox>
    private lateinit var dotnetProjectComboBox: Cell<AzureDotnetProjectComboBox>
    private lateinit var deploymentSlotGroup: CollapsibleRow
    private lateinit var deployToSlotCheckBox: Cell<JBCheckBox>
    private lateinit var newSlotRadioButton: Cell<JBRadioButton>
    private lateinit var existingSlotRadioButton: Cell<JBRadioButton>
    private lateinit var newSlotTextField: Cell<JBTextField>
    private lateinit var existingSlotNameComboBos: Cell<ComboBox<String>>
    private lateinit var slotConfigurationSourceComboBos: Cell<ComboBox<String>>
    private lateinit var openBrowserCheckBox: Cell<JBCheckBox>

    init {
        panel = panel {
            row("Project:") {
                dotnetProjectComboBox = dotnetProjectComboBox(project, ::canBePublishedToAzure)
                        .align(Align.FILL)
                        .resizableColumn()
            }
            row("Web App:") {
                webAppComboBox = webAppComboBox(project)
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

                    existingSlotNameComboBos = comboBox(emptyList<String>())
                            .enabledIf(deployToSlotCheckBox.selected.and(webAppComboBox.component.selectedValueMatches { it != null }))
                            .enabledIf(existingSlotRadioButton.selected)
                            .visibleIf(existingSlotRadioButton.selected)
                            .align(Align.FILL)

                }
                row("Slot Configuration Source:") {
                    slotConfigurationSourceComboBos = comboBox(emptyList<String>())
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

    }

    private fun loadAppSettings(value: WebAppConfig, before: WebAppConfig?) {

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

        data.webAppConfig?.let {
            webAppComboBox.component.setConfigModel(it)
            webAppComboBox.component.value = it
        }

        openBrowserCheckBox.component.isSelected = data.isOpenBrowserAfterDeployment
        deploymentSlotGroup.expanded = data.isSlotPanelVisible
    }

    override fun getValue(): WebAppDeployRunConfigurationModel {
        val project = dotnetProjectComboBox.component.value
        val artifactConfig = project?.let {
            AzureArtifactConfig.builder().artifactIdentifier(it.projectModelId.toString()).build()
        }
        val slotConfig: DeploymentSlotConfig? = null
        val webAppConfig = webAppComboBox.component.value
                ?.toBuilder()
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

    override fun getInputs() = listOf(webAppComboBox.component, dotnetProjectComboBox.component)

    private fun canBePublishedToAzure(publishableProject: PublishableProjectModel) =
            publishableProject.isWeb && (publishableProject.isDotNetCore || SystemInfo.isWindows)
}