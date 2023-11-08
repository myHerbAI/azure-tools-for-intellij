/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils

private const val AZURE_FUNCTIONS_APP_SETTINGS = "Azure Functions App Settings"

fun saveAppSettingsToSecurityStorage(key: String?, appSettings: Map<String, String>) {
    if (key.isNullOrEmpty()) {
        return
    }

    val appSettingsJsonValue = JsonUtils.toJson(appSettings)
    IntelliJSecureStore.getInstance().savePassword(AZURE_FUNCTIONS_APP_SETTINGS, key, null, appSettingsJsonValue)
}