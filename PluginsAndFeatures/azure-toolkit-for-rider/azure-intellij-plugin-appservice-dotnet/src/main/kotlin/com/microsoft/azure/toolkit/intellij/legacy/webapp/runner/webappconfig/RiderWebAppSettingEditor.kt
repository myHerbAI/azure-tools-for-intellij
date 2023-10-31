/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingsEditor

class RiderWebAppSettingEditor(project: Project, configuration: RiderWebAppConfiguration) :
        RiderAzureSettingsEditor<RiderWebAppConfiguration>() {
    private val panel = RiderWebAppSettingPanel(project, configuration)
    override fun getPanel() = panel
}