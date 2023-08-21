package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import lombok.Getter;

import java.util.Objects;
import java.util.Stack;

public class ChatBot {
    private final Stack<ChatMessage> chatMessages = new Stack<>();
    private final OpenAIClient client;
    @Getter
    private final CognitiveDeployment deployment;
    private String systemMessage;

    public ChatBot(final CognitiveDeployment deployment) {
        this.deployment = deployment;
        final CognitiveAccount account = deployment.getParent();
        final String endpoint = account.getEndpoint();
        final String primaryKey = Objects.requireNonNull(account.getPrimaryKey());
        this.client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(primaryKey))
            .buildClient();
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
        chatMessages.push(new ChatMessage(ChatRole.SYSTEM, systemMessage));
    }

    public ChatChoice send(String message) {
        chatMessages.push(new ChatMessage(ChatRole.USER, message));
        final String deploymentName = this.deployment.getName();
        final ChatCompletions chatCompletions = client.getChatCompletions(deploymentName, new ChatCompletionsOptions(chatMessages));
        final ChatChoice chatChoice = chatCompletions.getChoices().get(0);
        chatMessages.push(chatChoice.getMessage());
        return chatChoice;
    }

    public void reset() {
        chatMessages.clear();
        chatMessages.push(new ChatMessage(ChatRole.SYSTEM, systemMessage));
    }
}
