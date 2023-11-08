/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

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