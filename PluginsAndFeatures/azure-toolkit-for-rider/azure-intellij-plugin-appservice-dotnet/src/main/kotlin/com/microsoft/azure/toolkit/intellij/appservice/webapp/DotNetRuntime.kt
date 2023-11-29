/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.azure.resourcemanager.appservice.models.NetFrameworkVersion
import com.azure.resourcemanager.appservice.models.RuntimeStack
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem

data class DotNetRuntime(
    val operatingSystem: OperatingSystem,
    val stack: String?,
    val version: String?,
    val isDocker: Boolean
)

fun DotNetRuntime.toRuntimeStack(): RuntimeStack {
    val runtimeStack = requireNotNull(stack) { "Runtime stack cannot be null" }
    val runtimeVersion = requireNotNull(version) { "Runtime version cannot be null" }
    return RuntimeStack(runtimeStack, runtimeVersion)
}

fun DotNetRuntime.toNetFrameworkVersion(): NetFrameworkVersion {
    val runtimeVersion = requireNotNull(version) { "Runtime version cannot be null" }
    return NetFrameworkVersion.fromString("v$runtimeVersion")
}
