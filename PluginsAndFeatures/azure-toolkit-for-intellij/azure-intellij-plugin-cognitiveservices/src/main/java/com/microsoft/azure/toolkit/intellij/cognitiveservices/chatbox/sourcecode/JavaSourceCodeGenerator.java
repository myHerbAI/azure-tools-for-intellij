package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.azure.ai.openai.models.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Stack;
import java.util.stream.Collectors;

@Getter
public class JavaSourceCodeGenerator implements ISourceCodeGenerator {
    private final String name = "Java";
    private final String language = "java";

    @SneakyThrows
    @SuppressWarnings("deprecation")
    @Override
    public String generateCode(final ChatBot chatBot) {
        final CognitiveDeployment deployment = chatBot.getDeployment();
        final String endpoint = deployment.getParent().getEndpoint();
        final Configuration config = chatBot.getConfiguration();
        final Stack<ChatMessage> messages = chatBot.getChatMessages();
        final ObjectMapper mapper = new ObjectMapper();
        //noinspection deprecation
        final String msgs = messages.stream()
            .map(m -> String.format("        chatMessages.add(new ChatMessage(ChatRole.%s, \"%s\"));",
                m.getRole().toString().toUpperCase(), StringEscapeUtils.escapeJava(m.getContent())))
            .collect(Collectors.joining("\n"));

        return String.format("""
                import com.azure.ai.openai.OpenAIClient;
                import com.azure.ai.openai.OpenAIClientBuilder;
                import com.azure.ai.openai.models.ChatChoice;
                import com.azure.ai.openai.models.ChatCompletions;
                import com.azure.ai.openai.models.ChatCompletionsOptions;
                import com.azure.ai.openai.models.ChatMessage;
                import com.azure.ai.openai.models.ChatRole;
                import com.azure.core.credential.AzureKeyCredential;
                            
                import java.util.Arrays;
                import java.util.ArrayList;
                import java.util.List;
                            
                public class Example {
                    public static void main(String[] args) {
                        String endpoint = "%s";
                        String azureOpenaiKey = "YOUR_KEY";
                        String deploymentOrModelId = "%s";
                            
                        OpenAIClient client = new OpenAIClientBuilder()
                            .endpoint(endpoint)
                            .credential(new AzureKeyCredential(azureOpenaiKey))
                            .buildClient();
                            
                        List<ChatMessage> chatMessages = new ArrayList<>();
                %s

                        final ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
                        options.setMaxTokens(%d);
                        options.setTemperature(%.2f);
                        options.setFrequencyPenalty(%.1f);
                        options.setPresencePenalty(%.1f);
                        options.setTopP(%.2f);
                        options.setStop(Arrays.asList(%s));
                        ChatCompletions chatCompletions = client.getChatCompletions(deploymentOrModelId, options);
                            
                        for (ChatChoice choice : chatCompletions.getChoices()) {
                            ChatMessage message = choice.getMessage();
                            System.out.println("Message:");
                            System.out.println(message.getContent());
                        }
                    }
                }
                """, endpoint, deployment.getName(), msgs,
            config.getMaxResponse(),
            config.getTemperature(),
            config.getFrequencyPenalty(),
            config.getPresencePenalty(),
            config.getTopP(),
            config.getStopSequences().stream().map(s -> "\"" + StringEscapeUtils.escapeJava(s) + "\"")
                .collect(Collectors.joining(",")));
    }
}
