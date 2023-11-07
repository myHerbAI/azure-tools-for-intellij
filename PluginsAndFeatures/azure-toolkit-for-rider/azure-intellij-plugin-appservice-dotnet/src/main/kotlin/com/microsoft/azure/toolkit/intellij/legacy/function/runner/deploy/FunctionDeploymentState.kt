/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService

class FunctionDeploymentState(
    project: Project,
    private val functionDeploymentConfiguration: FunctionDeploymentConfiguration
) : RiderAzureRunProfileState<FunctionAppBase<*, *, *>>(project) {

    private val deployModel = functionDeploymentConfiguration.getModel()

    override fun executeSteps(processHandler: RunProcessHandler): FunctionAppBase<*, *, *> {
        val publishableProject = deployModel.publishableProject ?: throw RuntimeException("Project is not defined")
        val messenger = AzureMessager.getDefaultMessager()
        OperationContext.current().setMessager(processHandlerMessenger)
        val target = FunctionAppService.getInstance().createOrUpdateFunctionApp(deployModel.functionAppConfig)

        return target
    }

    override fun onSuccess(result: FunctionAppBase<*, *, *>, processHandler: RunProcessHandler) {
        TODO("Not yet implemented")
    }
}