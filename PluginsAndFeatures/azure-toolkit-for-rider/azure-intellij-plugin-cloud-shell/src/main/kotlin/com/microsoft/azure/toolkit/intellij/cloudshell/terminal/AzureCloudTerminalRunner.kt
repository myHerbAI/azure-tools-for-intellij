/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService
import com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel.CloudConsoleControlChannelWebSocket
import fleet.util.logging.logger
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner
import java.net.URI

class AzureCloudTerminalRunner(
    project: Project,
    private val cloudConsoleBaseUrl: String,
    socketUri: URI,
    process: AzureCloudTerminalProcess
) : CloudTerminalRunner(project, "Azure Cloud Shell", process) {
    companion object {
        private val LOG = logger<AzureCloudTerminalRunner>()
    }

    private val resizeTerminalUrl: String
    private val uploadFileToTerminalUrl: String
    private val previewPortBaseUrl: String
    private val controlChannelSocketUrl: String

    init {
        val builder = URIBuilder(socketUri).apply {
            scheme = "https"
            path = path.trimEnd('/')
        }

        val uri = builder.toString()
        resizeTerminalUrl = "$uri/size"
        uploadFileToTerminalUrl = "$uri/upload"
        previewPortBaseUrl = "$cloudConsoleBaseUrl/ports"
        controlChannelSocketUrl = "$socketUri/control"
    }

    override fun createTtyConnector(process: CloudTerminalProcess): TtyConnector {
        val cloudShellService = CloudShellService.getInstance(project)

        // Connect control socket
        // TODO: for now, this does not yet need any other wiring, it's just a convenience for e.g. downloading files
        val controlSocketClient = CloudConsoleControlChannelWebSocket(
            URI(controlChannelSocketUrl),
            cloudConsoleBaseUrl,
            project
        )
        controlSocketClient.connectBlocking()

        val connector = getConnector(process)
        cloudShellService.registerConnector(connector)

        return connector
    }

    private fun getConnector(process: CloudTerminalProcess): AzureCloudProcessTtyConnector =
        object : AzureCloudProcessTtyConnector(process) {
            override fun resize(termSize: TermSize) {
                if (!this.isConnected) return
                super.resize(termSize)
            }

            override fun uploadFile(fileName: String, file: VirtualFile) {
                TODO("Not yet implemented")
            }

            override fun openPreviewPort(port: Int, openInBrowser: Boolean) {
                TODO("Not yet implemented")
            }

            override fun closePreviewPort(port: Int) {
                TODO("Not yet implemented")
            }

            override fun getName() = "Connector: Azure Cloud Shell"

            override fun close() {
                val cloudShellService = CloudShellService.getInstance(project)
                cloudShellService.unregisterConnector(this)
                super.close()
            }
        }
}