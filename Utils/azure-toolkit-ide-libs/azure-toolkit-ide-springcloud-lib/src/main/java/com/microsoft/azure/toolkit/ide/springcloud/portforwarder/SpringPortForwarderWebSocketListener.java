/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud.portforwarder;

import com.microsoft.azure.toolkit.ide.common.portforwarder.AbstractPortForwarder;
import com.microsoft.azure.toolkit.ide.common.portforwarder.PortForwarderWebSocketListener;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import okhttp3.WebSocket;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class SpringPortForwarderWebSocketListener extends PortForwarderWebSocketListener {
    private int messagesRead = 0;

    public SpringPortForwarderWebSocketListener(ReadableByteChannel in, WritableByteChannel out, AbstractPortForwarder forwarder) {
        super(in, out, forwarder);
    }

    @Override
    protected int readMessage(@NotNull ReadableByteChannel channel, @NotNull ByteBuffer buffer) throws IOException {
        buffer.put((byte) 0);
        return super.readMessage(channel, buffer);
    }

    @Override
    protected void writeMessage(WebSocket webSocket, ByteString bytes) {
        ++this.messagesRead;
        final ByteBuffer buffer = bytes.asByteBuffer();
        if (this.messagesRead <= 2) {
            this.request();
            return;
        }
        if (!buffer.hasRemaining()) {
            this.closeWebSocket(webSocket, 1002, "Protocol error");
            AzureMessager.getMessager().error("Received an empty message.");
            return;
        }
        final byte channel = buffer.get();
        if (channel != 0) {
            this.closeWebSocket(webSocket, 1002, "Protocol error");
            final byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);
            final String errorMessage = new String(byteArray);
            AzureMessager.getMessager().error(errorMessage, "Received an error from the remote socket.");
            return;
        }
        super.writeMessage(webSocket, ByteString.of(buffer));
    }
}
