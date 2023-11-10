/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.settings

import com.intellij.openapi.components.*

@State(
    name = "com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings",
    storages = [(Storage("AzureSettings.xml"))]
)
@Service
class AzureFunctionSettings : SimplePersistentStateComponent<AzureFunctionSettingState>(AzureFunctionSettingState()) {
    companion object {
        fun getInstance(): AzureFunctionSettings = service()
    }

    var azureCoreToolsPathEntries: List<AzureCoreToolsPathEntry>
        get() {
            return buildList {
                add(AzureCoreToolsPathEntry("v4", state.functionV4Path ?: ""))
                add(AzureCoreToolsPathEntry("v3", state.functionV3Path ?: ""))
                add(AzureCoreToolsPathEntry("v2", state.functionV2Path ?: ""))
            }
        }
        set(entries) {
            for (entry in entries) {
                when(entry.functionsVersion) {
                    "v4" -> state.functionV4Path = entry.coreToolsPath
                    "v3" -> state.functionV3Path = entry.coreToolsPath
                    "v2" -> state.functionV2Path = entry.coreToolsPath
                    else -> continue
                }
            }
        }

    var functionDownloadPath
        get() = state.functionDownloadPath ?: ""
        set(value) {
            state.functionDownloadPath = value
        }
}

data class AzureCoreToolsPathEntry(var functionsVersion: String, var coreToolsPath: String)