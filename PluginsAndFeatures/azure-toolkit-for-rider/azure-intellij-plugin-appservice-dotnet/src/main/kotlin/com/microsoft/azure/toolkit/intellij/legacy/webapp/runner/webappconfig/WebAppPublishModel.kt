/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel
import java.util.*

class WebAppPublishModel : WebAppSettingModel() {
    var appSettingsKey: String = UUID.randomUUID().toString()
    var appSettings: Map<String, String> = mapOf()
    var isOpenBrowserAfterDeployment: Boolean = true
    var slotPanelVisible: Boolean = true
    var publishableProjectPath: String? = null
    var projectConfiguration: String = ""
    var projectPlatform: String = ""
}