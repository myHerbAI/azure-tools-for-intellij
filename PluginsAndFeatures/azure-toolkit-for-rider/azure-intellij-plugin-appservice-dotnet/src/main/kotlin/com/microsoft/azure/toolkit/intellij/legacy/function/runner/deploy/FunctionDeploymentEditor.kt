package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingsEditor

class FunctionDeploymentEditor(project: Project): RiderAzureSettingsEditor<FunctionDeploymentConfiguration>() {
    private val panel = FunctionDeploymentPanel(project)
    override fun getPanel() = panel
}