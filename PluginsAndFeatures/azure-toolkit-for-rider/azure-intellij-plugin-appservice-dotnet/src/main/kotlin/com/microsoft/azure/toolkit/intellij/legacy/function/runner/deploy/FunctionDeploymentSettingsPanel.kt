package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.configurationAndPlatformComboBox
import com.microsoft.azure.toolkit.intellij.common.dotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppComboBox
import com.microsoft.azure.toolkit.intellij.legacy.function.functionAppComboBox
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.canBePublishedToAzure
import java.util.*
import javax.swing.JPanel

class FunctionDeploymentSettingsPanel(private val project: Project, configuration: FunctionDeploymentConfiguration) :
    RiderAzureSettingPanel<FunctionDeploymentConfiguration>() {
    private val panel: JPanel
    private var appSettingsKey: String = configuration.appSettingsKey ?: UUID.randomUUID().toString()

    private lateinit var functionAppComboBox: Cell<FunctionAppComboBox>
    private lateinit var dotnetProjectComboBox: Cell<AzureDotnetProjectComboBox>
    private lateinit var configurationAndPlatformComboBox: Cell<LabeledComponent<ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>>>

    init {
        panel = panel {
            row("Function:") {
                functionAppComboBox = functionAppComboBox(project)
                    .align(Align.FILL)
            }
            row("Project:") {
                dotnetProjectComboBox = dotnetProjectComboBox(project) { it.canBePublishedToAzure() }
                    .align(Align.FILL)
                    .resizableColumn()
            }
            row("Configuration:") {
                configurationAndPlatformComboBox = configurationAndPlatformComboBox(project)
                    .align(Align.FILL)
            }
        }
    }

    override fun apply(configuration: FunctionDeploymentConfiguration) {
    }

    override fun reset(configuration: FunctionDeploymentConfiguration) {
    }

    override fun getMainPanel() = panel

    override fun disposeEditor() {
    }
}