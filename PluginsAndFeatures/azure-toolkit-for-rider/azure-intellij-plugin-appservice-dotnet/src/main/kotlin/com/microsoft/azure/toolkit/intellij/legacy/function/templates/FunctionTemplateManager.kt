/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.command.impl.DummyProject
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.use
import com.jetbrains.rider.projectView.actions.projectTemplating.backend.ReSharperProjectTemplateProvider
import java.io.File

@Service
class FunctionTemplateManager {
    companion object {
        fun getInstance(): FunctionTemplateManager = service()

        // Known and supported list of tags from https://github.com/Azure/azure-functions-tooling-feed/blob/main/cli-feed-v4.json
        val FUNCTIONS_CORETOOLS_KNOWN_SUPPORTED_VERSIONS = listOf("v2", "v3", "v4")

        // Latest supported version by the Azure Toolkit for Rider
        const val FUNCTIONS_CORETOOLS_LATEST_SUPPORTED_VERSION = "v4"

        private val LOG = logger<FunctionTemplateManager>()

        private fun isFunctionsProjectTemplate(file: File?) =
            file != null && file.name.startsWith("projectTemplates.", true) && file.name.endsWith(".nupkg", true)
    }

    fun areRegistered() =
        ReSharperProjectTemplateProvider.getUserTemplateSources().any { isFunctionsProjectTemplate(it) && it.exists() }

    fun tryReload() {
        // Determine core tools info for the latest supported Azure Functions version
        val coreToolsInfo = DummyProject.getInstance().use { dummyProject ->

        } ?: return

        // Remove previous templates
        ReSharperProjectTemplateProvider.getUserTemplateSources().forEach {
            if (isFunctionsProjectTemplate(it)) {
                ReSharperProjectTemplateProvider.removeUserTemplateSource(it)
            }
        }

        // Add available templates
//        val templateFolders = listOf(
//            File(coreToolsInfo.coreToolsPath).resolve("templates"), // Default worker
//            File(coreToolsInfo.coreToolsPath).resolve("templates").resolve("net5-isolated"), // Isolated worker - .NET 5
//            File(coreToolsInfo.coreToolsPath).resolve("templates").resolve("net6-isolated"), // Isolated worker - .NET 6
//            File(coreToolsInfo.coreToolsPath).resolve("templates").resolve("net-isolated")   // Isolated worker - .NET 5 - .NET 7
//        ).filter { it.exists() }
    }
}