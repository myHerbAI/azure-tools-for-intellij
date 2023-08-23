package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Configuration;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Stack;

public class ChatBot {
    private final Stack<ChatMessage> chatMessages = new Stack<>();
    @Getter
    private final CognitiveDeployment deployment;

    private String systemMessage;
    private Configuration configuration;
    private OpenAIClient client;

    public ChatBot(final CognitiveDeployment deployment) {
        this.deployment = deployment;
        this.refreshClient();
    }

    private OpenAIClient refreshClient() {
        final CognitiveAccount account = this.deployment.getParent();
        final String endpoint = account.getEndpoint();
        final String primaryKey = Objects.requireNonNull(account.getPrimaryKey());
        this.client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .configuration(this.configuration)
            .credential(new AzureKeyCredential(primaryKey))
            .buildClient();
        return this.client;
    }

    public void setSystemMessage(@Nonnull final String systemMessage) {
        this.systemMessage = systemMessage;
        this.chatMessages.clear();
        this.chatMessages.push(new ChatMessage(ChatRole.SYSTEM, systemMessage));
    }

    public void setConfiguration(@Nonnull final Configuration configuration) {
        this.configuration = configuration;
        this.refreshClient();
    }

    public ChatChoice send(String message) {
        this.chatMessages.push(new ChatMessage(ChatRole.USER, message));
        final String deploymentName = this.deployment.getName();
        final ChatCompletions chatCompletions = this.client.getChatCompletions(deploymentName, new ChatCompletionsOptions(this.chatMessages));
        final ChatChoice chatChoice = chatCompletions.getChoices().get(0);
        this.chatMessages.push(chatChoice.getMessage());
        return chatChoice;
    }

    public void reset() {
        this.chatMessages.clear();
        this.chatMessages.push(new ChatMessage(ChatRole.SYSTEM, this.systemMessage));
    }
}
