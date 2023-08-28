package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import java.util.function.Supplier
import javax.swing.JPanel
import kotlin.io.path.Path

class RiderAppServiceInfoBasicPanel<T>(
        project: Project,
        private val subscription: Subscription,
        private val defaultConfigSupplier: Supplier<T>
) : JPanel(), AzureFormPanel<T> where T : AppServiceConfig {

    private var config: T?

    private val panel: JPanel
    private val textName = AppNameInput().apply {
        isRequired = true
        setSubscription(subscription)
    }
    private val selectorApplication = AzureDotnetProjectComboBox(project) { _ -> true }

    private var operatingSystem: OperatingSystem
    private lateinit var windowsRadioButton: Cell<JBRadioButton>
    private lateinit var linuxRadioButton: Cell<JBRadioButton>

    private lateinit var deploymentGroup: Row


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
                    cell(selectorApplication)
                            .align(Align.FILL)
                }
            }
        }

        add(panel)

        setDeploymentVisible(false)
        config = defaultConfigSupplier.get().also { setValue(it) }
    }

    override fun getValue(): T {
        val name = textName.value
        val os = if (windowsRadioButton.component.isSelected) OperatingSystem.WINDOWS else OperatingSystem.LINUX
        val project = selectorApplication.value

        val result = config ?: defaultConfigSupplier.get()
        result.name = name
        result.runtime = Runtime(os, WebContainer("DOTNETCORE 7.0"), null)
        if (project != null) {
            result.application = Path(project.projectFilePath)
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
}