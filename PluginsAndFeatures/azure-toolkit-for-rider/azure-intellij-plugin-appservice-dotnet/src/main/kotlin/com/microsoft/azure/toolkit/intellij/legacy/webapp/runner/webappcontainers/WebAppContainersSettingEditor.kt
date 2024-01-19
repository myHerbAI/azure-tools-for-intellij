/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingsEditor

class WebAppContainersSettingEditor(project: Project) : RiderAzureSettingsEditor<WebAppContainersConfiguration>() {
    private val panel = WebAppContainersSettingPanel(project)

    init {
        Disposer.register(this, panel)
    }

    override fun getPanel() = panel
}