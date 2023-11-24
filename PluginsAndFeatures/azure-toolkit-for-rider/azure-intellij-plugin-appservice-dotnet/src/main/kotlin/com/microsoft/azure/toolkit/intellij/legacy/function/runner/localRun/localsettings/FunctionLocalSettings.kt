/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings

data class FunctionLocalSettings(
    val isEncrypted: Boolean?,
    val values: FunctionValuesModel?,
    val host: FunctionHostModel?,
    val connectionStrings: Map<String, String>?)

data class FunctionValuesModel(
    val workerRuntime: FunctionWorkerRuntime?,
    val webJobsStorage: String?,
    val webJobsDashboard: String?,
    val webJobsHttpExampleDisabled: Boolean?,
    val bindingConnection: String?)

data class FunctionHostModel(val localHttpPort: Int?, val cors: String?, val corsCredentials: Boolean?)

enum class FunctionWorkerRuntime(val value: String) {
    DotNetDefault("dotnet"),
    DotNetIsolated("dotnet-isolated")
}