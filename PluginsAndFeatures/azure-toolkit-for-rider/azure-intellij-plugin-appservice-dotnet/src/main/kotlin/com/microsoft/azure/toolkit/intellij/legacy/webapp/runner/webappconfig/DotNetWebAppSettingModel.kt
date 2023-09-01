package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel
import java.util.*

class DotNetWebAppSettingModel : WebAppSettingModel() {
    var appSettingsKey: String = UUID.randomUUID().toString()
    var appSettings: Map<String, String> = mapOf()
    var appSettingsToRemove: Set<String> = setOf()
    var isOpenBrowserAfterDeployment: Boolean = true
    var slotPanelVisible: Boolean = true
    var projectPath: String = ""
    var projectConfiguration: String = ""
    var projectPlatform: String = ""
}