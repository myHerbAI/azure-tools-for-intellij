/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.settings

import com.intellij.openapi.components.*
import java.nio.file.Path

@State(
    name = "com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings",
    storages = [(Storage("AzureSettings.xml"))]
)
@Service
class AzureFunctionSettings : SimplePersistentStateComponent<AzureFunctionSettingState>(
    AzureFunctionSettingState(
        Path.of(System.getProperty("user.home")).resolve(AZURE_TOOLS_FOLDER).resolve(AZURE_FUNCTIONS_TOOLS_FOLDER)
    )
) {
    companion object {
        fun getInstance() = service<AzureFunctionSettings>()

        const val DISMISS_NOTIFICATION_AZURE_FUNCTIONS_MISSING_NUPKG = "DismissAzureFunctionsMissingNupkg"

        private const val AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ"
        private const val AZURE_FUNCTIONS_TOOLS_FOLDER = "AzureFunctionsCoreTools"
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
                when (entry.functionsVersion) {
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