/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.azure.resourcemanager.appservice.models.OperatingSystem
import com.azure.resourcemanager.appservice.models.WebAppBase

fun WebAppBase.getDotNetRuntime(): DotNetRuntime {
    val os = operatingSystem()
    if (os == OperatingSystem.LINUX) {
        return DotNetRuntime(
            com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.LINUX,
            null,
            null,
            false
        )
    } else {
        return DotNetRuntime(
            com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.WINDOWS,
            null,
            null,
            false
        )
    }
}