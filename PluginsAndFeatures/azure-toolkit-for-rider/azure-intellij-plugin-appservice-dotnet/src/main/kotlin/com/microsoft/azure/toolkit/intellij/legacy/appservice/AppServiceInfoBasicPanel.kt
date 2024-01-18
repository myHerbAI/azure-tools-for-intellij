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
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.configurationAndPlatformComboBox
import com.microsoft.azure.toolkit.intellij.common.dotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.legacy.canBePublishedToAzure
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_CONFIGURATION
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog.Companion.RIDER_PROJECT_PLATFORM
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import java.util.function.Supplier
import javax.swing.JPanel
import kotlin.io.path.Path

class AppServiceInfoBasicPanel<T>(
    private val project: Project,
    private val subscription: Subscription,
    private val defaultConfigSupplier: Supplier<T>
) : JPanel(), Disposable, AzureFormPanel<T> where T : AppServiceConfig {

    private var config: T?

    private val panel: JPanel
    private val textName = AppNameInput().apply {
        isRequired = true
        setSubscription(subscription)
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
            group("Instance Details") {
                row("Name:") {
                    cell(textName)
                    label(".azurewebsites.net")
                }
                operatingSystemGroup = buttonsGroup {
                    row("Operating System:") {
                        linuxRadioButton = radioButton("Linux", OperatingSystem.LINUX)
                        windowsRadioButton = radioButton("Windows", OperatingSystem.WINDOWS)
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
        setDeploymentVisible(false)
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
        result.runtime = Runtime(os, WebContainer.JAVA_OFF, JavaVersion.OFF)

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