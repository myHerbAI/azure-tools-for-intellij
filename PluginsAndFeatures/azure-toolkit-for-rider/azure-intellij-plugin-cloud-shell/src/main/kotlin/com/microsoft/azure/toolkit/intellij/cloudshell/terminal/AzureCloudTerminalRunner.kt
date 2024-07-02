/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellService
import com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel.CloudConsoleControlChannelWebSocket
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner
import java.net.URI

class AzureCloudTerminalRunner(
    project: Project,
    private val scope: CoroutineScope,
    private val cloudConsoleBaseUrl: String,
    socketUri: String,
    process: AzureCloudTerminalProcess
) : CloudTerminalRunner(project, "Azure Cloud Shell", process) {
    companion object {
        private val LOG = logger<AzureCloudTerminalRunner>()
    }

    private val controlChannelSocketUrl = "$socketUri/control"

    private val resizeTerminalUrl: String
    private val uploadFileToTerminalUrl: String
    private val previewPortBaseUrl: String

    init {
        val builder = URIBuilder(socketUri).apply {
            scheme = "https"
            path = path.removePrefix("/\$hc").trimEnd('/')
        }
        val uri = builder.toString()
        resizeTerminalUrl = "$uri/size"
        uploadFileToTerminalUrl = "$uri/upload"
        previewPortBaseUrl = "$cloudConsoleBaseUrl/ports"
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
                val cloudConsoleService = CloudConsoleService.getInstance(project)
                scope.launch {
                    val result = cloudConsoleService.resizeTerminal(resizeTerminalUrl, termSize.columns, termSize.rows)
                    if (!result) {
                        LOG.warn("Could not resize cloud terminal")
                    }
                }
            }

            override fun uploadFile(fileName: String, file: VirtualFile) {
                val cloudConsoleService = CloudConsoleService.getInstance(project)
            }

            override fun openPreviewPort(port: Int, openInBrowser: Boolean) {
                val cloudConsoleService = CloudConsoleService.getInstance(project)
                scope.launch {
                    val result = cloudConsoleService.openPreviewPort("$previewPortBaseUrl/$port/open")
                    if (result == null) {
                        LOG.warn("Could not open preview port")
                        return@launch
                    }

                    openPreviewPorts.add(port)

                    val url = result.url
                    if (openInBrowser && url != null) {
                        BrowserUtil.open(url)
                    }
                }
            }

            override fun closePreviewPort(port: Int) {
                val cloudConsoleService = CloudConsoleService.getInstance(project)
                scope.launch {
                    val result = cloudConsoleService.closePreviewPort("$previewPortBaseUrl/$port/close")
                    if (result == null) {
                        LOG.warn("Could not close preview port")
                        return@launch
                    }

                    openPreviewPorts.remove(port)
                }
            }

            override fun getName() = "Connector: Azure Cloud Shell"

            override fun close() {
                val cloudShellService = CloudShellService.getInstance(project)
                cloudShellService.unregisterConnector(this)
                super.close()
            }
        }
}