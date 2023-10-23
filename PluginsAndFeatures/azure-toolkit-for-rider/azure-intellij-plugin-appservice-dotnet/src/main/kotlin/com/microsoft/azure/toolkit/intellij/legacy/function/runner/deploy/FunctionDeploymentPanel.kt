package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import javax.swing.JPanel

class FunctionDeploymentPanel(private val project: Project) : RiderAzureSettingPanel<FunctionDeploymentConfiguration>() {
    private val panel: JPanel

    init {
        panel = panel {

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