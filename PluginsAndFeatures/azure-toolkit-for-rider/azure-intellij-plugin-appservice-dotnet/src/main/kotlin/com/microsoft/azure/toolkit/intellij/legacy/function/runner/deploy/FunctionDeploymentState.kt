/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase

class FunctionDeploymentState(
    project: Project,
    private val functionDeploymentConfiguration: FunctionDeploymentConfiguration
) : RiderAzureRunProfileState<FunctionAppBase<*, *, *>>(project) {
    override fun executeSteps(processHandler: RunProcessHandler): FunctionAppBase<*, *, *> {
        TODO("Not yet implemented")
    }

    override fun onSuccess(result: FunctionAppBase<*, *, *>, processHandler: RunProcessHandler) {
        TODO("Not yet implemented")
    }
}