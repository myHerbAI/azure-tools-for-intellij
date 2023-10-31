package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime

class WebAppContainersCreationDialog(project: Project) : WebAppCreationDialog(project) {
    init {
        basicFormPanel.setFixedRuntime(Runtime.DOCKER)
        advancedFormPanel.setFixedRuntime(Runtime.DOCKER)
    }
}