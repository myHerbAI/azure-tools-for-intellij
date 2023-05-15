/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

@Slf4j
public class WebSocketSSLProxy {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private final AppServiceAppBase<?, ?, ?> app;

    @Setter
    @Getter
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    @Setter
    @Getter
    private int connectTimeout = 0;

    private final String webSocketServerUri;
    private ServerSocket serverSocket;
    private Thread thread;
    private WebSocket webSocket;

    public WebSocketSSLProxy(String webSocketServerUri, AppServiceAppBase<?, ?, ?> appService) {
        this.webSocketServerUri = webSocketServerUri;
        this.app = appService;
    }

    public void start() throws IOException {
        close();
        // InetAddress.getByName(null) points to the loopback address (127.0.0.1)
        serverSocket = new ServerSocket(0, 1, InetAddress.getByName(null));
        thread = new Thread(() -> {
            try {
                for (; ; ) {
                    final Socket clientSocket = serverSocket.accept();
                    createWebSocketToSocket(clientSocket);
                    pipeSocketDataToWebSocket(clientSocket);
                }
            } catch (final IOException | WebSocketException e) {
                handleConnectionBroken(e);
            }

        });
        thread.setName("WebsocketSSLProxy-" + thread.getId());
        thread.start();
    }

    public void close() {
        if (this.webSocket != null) {
            this.webSocket.disconnect();
            this.webSocket = null;
        }
        if (this.serverSocket != null) {
            try {
                serverSocket.close();
            } catch (final IOException e) {
                // ignore
            }
            serverSocket = null;
        }

        if (thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
    }

    public int getLocalPort() {
        if (Objects.isNull(serverSocket)) {
            return 0;
        }
        return serverSocket.getLocalPort();
    }

    private void handleConnectionBroken(Exception e) {
        if (Objects.nonNull(serverSocket)) {
            log.warn(message("common.webSocket.error.proxyingWebSocketFailed", e.getMessage()));
        }
        close();
    }

    private void pipeSocketDataToWebSocket(Socket socket) throws IOException {
        final byte[] buffer = new byte[bufferSize];
        while (true) {
            final int bytesRead = socket.getInputStream().read(buffer);
            if (bytesRead == -1) {
                break;
            }

            webSocket.sendBinary(Arrays.copyOfRange(buffer, 0, bytesRead));
        }
    }

    private void createWebSocketToSocket(Socket client) throws IOException, WebSocketException {
        final Account account = Azure.az(AzureAccount.class).account();
        final String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = (new TokenRequestContext()).addScopes(scopes);
        final String accessToken = account.getTokenCredential(this.app.getSubscriptionId()).getToken(request).block().getToken();

        final WebSocket socket = new WebSocketFactory().setConnectionTimeout(connectTimeout).createSocket(webSocketServerUri);
        socket.addHeader("Authorization", "Bearer " + accessToken);
        this.webSocket = socket
            .addListener(new WebSocketAdapter() {
                @Override
                public void onBinaryMessage(WebSocket websocket, byte[] bytes) {
                    try {
                        client.getOutputStream().write(bytes);
                    } catch (final IOException e) {
                        handleConnectionBroken(e);
                    }
                }
            }).addExtension(WebSocketExtension.PERMESSAGE_DEFLATE).connect();
    }
}
