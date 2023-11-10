/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

@Service
class FunctionsCoreToolsInfoProvider {
    companion object {
        fun getInstance(): FunctionsCoreToolsInfoProvider = service()

        private val LOG = logger<FunctionsCoreToolsInfoProvider>()
    }

    fun retrieveForVersion(
        project: Project,
        azureFunctionsVersion: String,
        allowDownload: Boolean
    ): FunctionsCoreToolsInfo? {
        return null
    }

    private fun retrieveFromConfiguration(azureFunctionsVersion: String): FunctionsCoreToolsInfo? {
        return null
    }
}