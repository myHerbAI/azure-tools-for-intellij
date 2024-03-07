/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.ButtonsGroup
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppLinuxRuntime
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppWindowsRuntime
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import java.util.function.Supplier
import javax.swing.JPanel

class AppServiceInfoBasicPanel<T>(
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
        }

        add(panel)

        config = defaultConfigSupplier.get().also { setValue(it) }
    }

    override fun getValue(): T {
        val name = textName.value
        val os = if (windowsRadioButton.component.isSelected) OperatingSystem.WINDOWS else OperatingSystem.LINUX

        val result = config ?: defaultConfigSupplier.get()
        result.name = name
        result.runtime = if (os == OperatingSystem.LINUX) WebAppLinuxRuntime.JAVASE_JAVA17 else WebAppWindowsRuntime.JAVASE_JAVA17

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

    fun setFixedRuntime(runtime: Runtime) {
        if (runtime.operatingSystem == OperatingSystem.WINDOWS) {
            windowsRadioButton.component.isSelected = true
        } else {
            linuxRadioButton.component.isSelected = true
        }
        operatingSystemGroup.visible(false)
    }

    override fun dispose() {
    }
}