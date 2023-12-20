/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.rider.projectView.actions.projectTemplating.backend.ReSharperProjectTemplateProvider
import com.microsoft.azure.toolkit.intellij.legacy.function.FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfoProvider
import java.io.File

@Service
class FunctionTemplateManager {
    companion object {
        fun getInstance(): FunctionTemplateManager = service()

        private val LOG = logger<FunctionTemplateManager>()

        private fun isFunctionsProjectTemplate(file: File?) =
            file != null && file.name.startsWith("projectTemplates.", true) && file.name.endsWith(".nupkg", true)
    }

    fun areRegistered() =
        ReSharperProjectTemplateProvider.getUserTemplateSources().any { isFunctionsProjectTemplate(it) && it.exists() }

    suspend fun tryReload() {
        ThreadingAssertions.assertBackgroundThread()

        // Determine core tools info for the latest supported Azure Functions version
        val toolsInfoProvider = FunctionCoreToolsInfoProvider.getInstance()
        val coreToolsInfo = toolsInfoProvider.retrieveForVersion(
            FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION,
            false
        ) ?: return

        // Remove previous templates
        ReSharperProjectTemplateProvider.getUserTemplateSources().forEach {
            if (isFunctionsProjectTemplate(it)) {
                ReSharperProjectTemplateProvider.removeUserTemplateSource(it)
            }
        }

        // Add available templates
        val templateFolders = listOf(
            File(coreToolsInfo.coreToolsPath)
                .resolve("templates"), // Default worker
            File(coreToolsInfo.coreToolsPath)
                .resolve("templates").resolve("net5-isolated"), // Isolated worker - .NET 5
            File(coreToolsInfo.coreToolsPath)
                .resolve("templates").resolve("net6-isolated"), // Isolated worker - .NET 6
            File(coreToolsInfo.coreToolsPath)
                .resolve("templates").resolve("net-isolated")   // Isolated worker - .NET 5 - .NET 8
        ).filter { it.exists() }

        for (templateFolder in templateFolders) {
            try {
                val templateFiles = templateFolder.listFiles { f: File? -> isFunctionsProjectTemplate(f) }
                    ?: emptyArray<File>()

                LOG.info("Found ${templateFiles.size} function template(s) in ${templateFolder.path}")

                templateFiles.forEach { file ->
                    ReSharperProjectTemplateProvider.addUserTemplateSource(file)
                }
            } catch (e: Exception) {
                LOG.error("Could not register project templates from ${templateFolder.path}", e)
            }
        }
    }
}