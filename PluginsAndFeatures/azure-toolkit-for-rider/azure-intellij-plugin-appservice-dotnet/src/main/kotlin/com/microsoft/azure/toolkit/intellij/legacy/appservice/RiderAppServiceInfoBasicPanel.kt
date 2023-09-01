package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.configurationAndPlatformComboBox
import com.microsoft.azure.toolkit.intellij.common.dotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.legacy.webapp.RiderWebAppCreationDialog.Companion.RIDER_PROJECT_CONFIGURATION
import com.microsoft.azure.toolkit.intellij.legacy.webapp.RiderWebAppCreationDialog.Companion.RIDER_PROJECT_PLATFORM
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.canBePublishedToAzure
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import java.util.function.Supplier
import javax.swing.JPanel
import kotlin.io.path.Path

class RiderAppServiceInfoBasicPanel<T>(
        private val project: Project,
        private val subscription: Subscription,
        private val defaultConfigSupplier: Supplier<T>
) : JPanel(), AzureFormPanel<T> where T : AppServiceConfig {

    private var config: T?

    private val panel: JPanel
    private val textName = AppNameInput().apply {
        isRequired = true
        setSubscription(subscription)
    }

    private var operatingSystem: OperatingSystem
    private lateinit var windowsRadioButton: Cell<JBRadioButton>
    private lateinit var linuxRadioButton: Cell<JBRadioButton>
    private lateinit var deploymentGroup: Row
    private lateinit var dotnetProjectComboBox: Cell<AzureDotnetProjectComboBox>
    private lateinit var configurationAndPlatformComboBox: Cell<LabeledComponent<ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>>>

    init {
        operatingSystem = OperatingSystem.WINDOWS

        panel = panel {
            group("Instance Details") {
                row("Name:") {
                    cell(textName)
                    label(".azurewebsites.net")
                }
                buttonsGroup {
                    row("Operating System:") {
                        windowsRadioButton = radioButton("Windows", OperatingSystem.WINDOWS)
                        linuxRadioButton = radioButton("Linux", OperatingSystem.LINUX)
                    }
                }.bind(::operatingSystem)
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

        config = defaultConfigSupplier.get().also { setValue(it) }
    }

    override fun getValue(): T {
        val name = textName.value
        val os = if (windowsRadioButton.component.isSelected) OperatingSystem.WINDOWS else OperatingSystem.LINUX
        val projectModel = dotnetProjectComboBox.component.value
        val configurationAndPlatform = getSelectedConfigurationAndPlatform()

        val result = config ?: defaultConfigSupplier.get()
        result.name = name
        result.runtime = Runtime(os, WebContainer("DOTNETCORE 7.0"), null)

        if (projectModel != null) {
            result.application = Path(projectModel.projectFilePath)
        }
        if (configurationAndPlatform != null) {
            result.appSettings[RIDER_PROJECT_CONFIGURATION] = configurationAndPlatform.configuration
            result.appSettings[RIDER_PROJECT_PLATFORM] = configurationAndPlatform.platform
        }

        config = result

        return result
    }

    override fun setValue(config: T) {
        this.config = config
        textName.value = config.name
        textName.setSubscription(config.subscription)
        if (config.runtime.operatingSystem == OperatingSystem.WINDOWS) {
            windowsRadioButton.component.isSelected = true
        } else {
            linuxRadioButton.component.isSelected = true
        }
    }

    override fun getInputs(): List<AzureFormInput<*>> = listOf(textName)

    override fun setVisible(visible: Boolean) {
        panel.isVisible = visible
        super<JPanel>.setVisible(visible)
    }

    fun setDeploymentVisible(visible: Boolean) {
        deploymentGroup.visible(visible)
    }

    private fun getSelectedConfigurationAndPlatform(): PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform? =
            configurationAndPlatformComboBox.component.component.selectedItem as? PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform
}