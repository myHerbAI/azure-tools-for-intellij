package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

public class ChatBot {
    @Getter
    private final Stack<ChatMessage> chatMessages = new Stack<>();
    @Getter
    private final CognitiveDeployment deployment;
    private SystemMessage systemMessage;
    @Getter
    private Configuration configuration;
    private OpenAIClient client;
    @Getter
    private String primaryKey;

    public ChatBot(final CognitiveDeployment deployment) {
        this.deployment = deployment;
        this.refreshClient();
    }

    private void refreshClient() {
        final CognitiveAccount account = this.deployment.getParent();
        final String endpoint = account.getEndpoint();
        this.primaryKey = Objects.requireNonNull(account.getPrimaryKey());
        this.client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(primaryKey))
            .buildClient();
    }

    public void setSystemMessage(@Nonnull final String systemMessage) {
        this.systemMessage = SystemMessage.builder().systemMessage(systemMessage).build();
        this.chatMessages.clear();
        this.chatMessages.push(new ChatMessage(ChatRole.SYSTEM, systemMessage));
    }

    public void setSystemMessage(@Nonnull final SystemMessage message) {
        this.systemMessage = message;
        this.chatMessages.clear();
        this.chatMessages.push(new ChatMessage(ChatRole.SYSTEM, message.getSystemMessage()));
        for (final SystemMessage.Example example : message.getExamples()) {
            this.chatMessages.push(new ChatMessage(ChatRole.USER, example.getUser()));
            this.chatMessages.push(new ChatMessage(ChatRole.ASSISTANT, example.getAssistant()));
        }
    }

    public void setConfiguration(@Nonnull final Configuration configuration) {
        this.configuration = configuration;
        this.refreshClient();
    }

    public ChatChoice send(String message) {
        this.chatMessages.push(new ChatMessage(ChatRole.USER, message));
        final String deploymentName = this.deployment.getName();
        final ChatCompletions chatCompletions = this.client.getChatCompletions(deploymentName, buildChatOptions());
        final ChatChoice chatChoice = chatCompletions.getChoices().get(0);
        this.chatMessages.push(chatChoice.getMessage());
        return chatChoice;
    }

    private ChatCompletionsOptions buildChatOptions() {
        final ChatCompletionsOptions result = new ChatCompletionsOptions(this.chatMessages);
        Optional.ofNullable(configuration).ifPresent(c -> {
            result.setFrequencyPenalty(c.getFrequencyPenalty());
            result.setPresencePenalty(c.getPresencePenalty());
            result.setStop(c.getStopSequences());
            result.setTemperature(c.getTemperature());
            result.setTopP(c.getTopP());
            result.setMaxTokens(c.getMaxResponse());
        });
        return result;
    }

    public void reset() {
        setSystemMessage(this.systemMessage);
    }
}
