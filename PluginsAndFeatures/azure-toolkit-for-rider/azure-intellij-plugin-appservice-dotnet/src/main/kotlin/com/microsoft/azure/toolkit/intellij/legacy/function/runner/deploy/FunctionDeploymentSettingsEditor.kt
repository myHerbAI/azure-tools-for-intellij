/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingsEditor

class FunctionDeploymentSettingsEditor(project: Project, configuration: FunctionDeploymentConfiguration) :
    RiderAzureSettingsEditor<FunctionDeploymentConfiguration>() {

    private val panel = FunctionDeploymentSettingsPanel(project, configuration)
    override fun getPanel() = panel
}