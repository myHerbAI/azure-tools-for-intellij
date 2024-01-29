/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.azure.ai.openai.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Getter
@RequiredArgsConstructor
@Slf4j
public class ChatMessage {
    private final ChatRole role;
    private final String content;

    public static ChatMessage fromResponse(ChatResponseMessage response) {
        return new ChatMessage(response.getRole(), response.getContent());
    }

    @Nullable
    public ChatRequestMessage toRequest() {
        if (this.role == ChatRole.SYSTEM) return new ChatRequestSystemMessage(this.content);
        if (this.role == ChatRole.USER) return new ChatRequestUserMessage(this.content);
        if (this.role == ChatRole.ASSISTANT) return new ChatRequestAssistantMessage(this.content);
        log.warn("Unknown role: {}", this.role);
        return null;
    }
}
