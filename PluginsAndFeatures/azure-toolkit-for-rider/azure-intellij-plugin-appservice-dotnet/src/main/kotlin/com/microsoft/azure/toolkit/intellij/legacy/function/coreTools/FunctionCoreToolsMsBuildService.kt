/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rider.azure.model.AzureFunctionsVersionRequest
import com.jetbrains.rider.azure.model.functionAppDaemonModel
import com.jetbrains.rider.projectView.solution

@Service
class FunctionCoreToolsMsBuildService {
    companion object {
        fun getInstance() = service<FunctionCoreToolsMsBuildService>()
        const val PROPERTY_AZURE_FUNCTIONS_VERSION = "AzureFunctionsVersion"
    }

    suspend fun requestAzureFunctionsVersion(project: Project, projectFilePath: String): String? {
        val functionVersion = project.solution.functionAppDaemonModel.getAzureFunctionsVersion
            .startSuspending(AzureFunctionsVersionRequest(projectFilePath))

        //hack: sometimes we receive v0 even for v4 project
        if (functionVersion == "v0") {
            return "v4"
        }

        return functionVersion
    }
}