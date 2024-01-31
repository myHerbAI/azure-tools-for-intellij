/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.azurite.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.util.io.FileUtil

@State(
    name = "com.microsoft.azure.toolkit.intellij.azurite.settings.AzuriteSettings",
    storages = [(Storage("AzureSettings.xml"))]
)
@Service
class AzuriteSettings : SimplePersistentStateComponent<AzuriteSettingsState>(AzuriteSettingsState()) {
    companion object {
        fun getInstance() = service<AzuriteSettings>()
    }

    var executablePath
        get() = state.executablePath ?: ""
        set(value) {
            state.executablePath = FileUtil.toSystemIndependentName(value)
        }

    var locationMode
        get() = state.locationMode
        set(value) {
            state.locationMode = value
        }

    var workspacePath
        get() = state.workspacePath ?: ""
        set(value) {
            state.workspacePath = FileUtil.toSystemIndependentName(value)
        }

    var looseMode
        get() = state.looseMode
        set(value) {
            state.looseMode = value
        }

    var blobHost
        get() = state.blobHost ?: ""
        set(value) {
            state.blobHost = value
        }

    var blobPort
        get() = state.blobPort
        set(value) {
            state.blobPort = value
        }

    var queueHost
        get() = state.queueHost ?: ""
        set(value) {
            state.queueHost = value
        }

    var queuePort
        get() = state.queuePort
        set(value) {
            state.queuePort = value
        }

    var tableHost
        get() = state.tableHost ?: ""
        set(value) {
            state.tableHost = value
        }

    var tablePort
        get() = state.tablePort
        set(value) {
            state.tablePort = value
        }

    var certificatePath
        get() = state.certificatePath ?: ""
        set(value) {
            state.certificatePath = FileUtil.toSystemIndependentName(value)
        }

    var certificateKeyPath
        get() = state.certificateKeyPath ?: ""
        set(value) {
            state.certificateKeyPath = FileUtil.toSystemIndependentName(value)
        }

    var certificatePassword
        get() = state.certificatePassword ?: ""
        set(value) {
            state.certificatePassword = value
        }
}