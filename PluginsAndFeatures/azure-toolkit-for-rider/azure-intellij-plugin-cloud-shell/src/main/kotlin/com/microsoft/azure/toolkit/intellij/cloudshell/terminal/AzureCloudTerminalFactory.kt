/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.ide.ui.IdeUiService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleTerminalWebSocket
import java.net.URI

@Service(Service.Level.PROJECT)
class AzureCloudTerminalFactory(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<AzureCloudTerminalFactory>()
    }

    fun createTerminalRunner(cloudConsoleBaseUrl: String, socketUri: URI): AzureCloudTerminalRunner {
        val terminalSocketClient = CloudConsoleTerminalWebSocket(socketUri)
        IdeUiService.getInstance().sslSocketFactory?.let {
            // Inject IDEA SSL socket factory
            terminalSocketClient.setSocketFactory(it)
        }

        terminalSocketClient.connectBlocking()

        return AzureCloudTerminalRunner(
            project,
            cloudConsoleBaseUrl,
            socketUri,
            AzureCloudTerminalProcess(terminalSocketClient)
        )
    }
}