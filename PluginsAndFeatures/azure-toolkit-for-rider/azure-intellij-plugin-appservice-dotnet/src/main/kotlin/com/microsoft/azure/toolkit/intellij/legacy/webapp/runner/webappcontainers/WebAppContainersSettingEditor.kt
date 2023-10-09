package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingsEditor

class WebAppContainersSettingEditor(project: Project, configuration: WebAppContainersConfiguration) :
        RiderAzureSettingsEditor<WebAppContainersConfiguration>() {

    private val panel = WebAppContainersSettingPanel(project, configuration)

    override fun getPanel() = panel
}