/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configurations.RunProfile
import com.jetbrains.rider.debugger.DotNetProgramRunner

class FunctionProgramRunner: DotNetProgramRunner() {
    override fun canRun(executorId: String, runConfiguration: RunProfile) = runConfiguration is FunctionRunConfiguration
}