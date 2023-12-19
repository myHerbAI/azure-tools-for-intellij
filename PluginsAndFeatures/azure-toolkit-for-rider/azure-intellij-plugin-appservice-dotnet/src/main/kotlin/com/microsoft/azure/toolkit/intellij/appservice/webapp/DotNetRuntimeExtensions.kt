/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.azure.resourcemanager.appservice.models.OperatingSystem
import com.azure.resourcemanager.appservice.models.RuntimeStack
import com.azure.resourcemanager.appservice.models.WebAppBase
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

fun WebAppBase.getDotNetRuntime(): DotNetRuntime {
    val os = operatingSystem()
    if (os == OperatingSystem.LINUX) {
        val linuxFxVersion = linuxFxVersion()
        if (linuxFxVersion.startsWith("docker", true)) {
            return DotNetRuntime(
                com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.LINUX,
                null,
                null,
                true
            )
        } else {
            val stack = linuxFxVersion.substringBefore('|', "DOTNETCORE")
            val version = linuxFxVersion.substringAfter('|', "8.0")
            return DotNetRuntime(
                com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.LINUX,
                RuntimeStack(stack, version),
                null,
                false
            )
        }
    } else {
        return DotNetRuntime(
            com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.WINDOWS,
            null,
            netFrameworkVersion(),
            false
        )
    }
}

fun AppServiceAppBase<*, *, *>.getDotNetRuntime() = getRemote()?.getDotNetRuntime()