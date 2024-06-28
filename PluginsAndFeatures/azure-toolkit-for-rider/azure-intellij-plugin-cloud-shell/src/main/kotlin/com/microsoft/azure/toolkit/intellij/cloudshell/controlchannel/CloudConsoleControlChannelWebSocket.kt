/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel

import com.intellij.openapi.project.Project
import fleet.util.logging.logger
import kotlinx.serialization.json.Json
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import kotlin.Exception

class CloudConsoleControlChannelWebSocket(
    serverURI: URI,
    private val cloudConsoleBaseUrl: String,
    private val project: Project
) : WebSocketClient(serverURI) {
    companion object {
        private val LOG = logger<CloudConsoleControlChannelWebSocket>()
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun onOpen(handshakedata: ServerHandshake?) {}

    override fun onMessage(message: String?) {
        if (message != null) {
            try {
                val controlMessage = json.decodeFromString<ControlMessage>(message)
                when (controlMessage.audience) {
                    "download" -> {
                        DownloadControlMessageHandler(cloudConsoleBaseUrl, project).handle(message)
                    }
                    "url" -> {
                        UrlControlMessageHandler(project).handle(message)
                    }
                }
            } catch (e: Exception) {
                LOG.error(e, "Error on receiving message from cloud shell terminal")
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {}

    override fun onError(ex: Exception?) {}
}