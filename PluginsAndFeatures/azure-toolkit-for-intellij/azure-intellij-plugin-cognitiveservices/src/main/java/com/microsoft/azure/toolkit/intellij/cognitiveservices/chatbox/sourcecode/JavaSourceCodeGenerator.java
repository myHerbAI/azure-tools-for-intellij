package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import lombok.Getter;

@Getter
public class JavaSourceCodeGenerator implements ISourceCodeGenerator {
    private final String name = "Java";
    private final String language = "java";

    @Override
    public String generateCode(final ChatBot chatBot) {
        return """
            // Note: The Azure OpenAI client library for .NET is in preview.
            // Install the .NET library via NuGet: dotnet add package Azure.AI.OpenAI --version 1.0.0-beta.5\s
            using Azure;
                        
            using Azure.AI.OpenAI;
                        
            OpenAIClient client = new OpenAIClient(
            	new Uri("https://eastus.api.cognitive.microsoft.com/"),
            	new AzureKeyCredential(Environment.GetEnvironmentVariable("AZURE_OPENAI_API_KEY")));
                        
            // ### If streaming is selected
            Response<StreamingChatCompletions> response = await client.GetChatCompletionsStreamingAsync(
            	deploymentOrModelName: "deployment-230822104637",
            	new ChatCompletionsOptions()
            	{
            		Messages =
            		{
            			new ChatMessage(ChatRole.System, @"You are a java expert."),
            			new ChatMessage(ChatRole.User, @"hello, world in java."),
            			new ChatMessage(ChatRole.Assistant, @"Hello! Here's the \\"Hello, World!\\" program in Java:
                        
            ```java
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println(\\"Hello, World!\\");
                }
            }
            ```
                        
            To run this program, save it to a file called `HelloWorld.java`, then open a terminal or command prompt and navigate to the directory where the file is located. Use the following command to compile and run the program:
                        
            ```
            javac HelloWorld.java
            java HelloWorld
            ```
                        
            You should see the output `Hello, World!` printed to the console. Let me know if you have any further questions!"),
            			new ChatMessage(ChatRole.User, @"hello, world in c#"),
            			new ChatMessage(ChatRole.Assistant, @"Certainly! Here's the \\"Hello, World!\\" program in C#:
                        
            ```csharp
            using System;
                        
            class HelloWorld {
                static void Main() {
                    Console.WriteLine(\\"Hello, World!\\");
                }
            }
            ```
                        
            To run this program, save it to a file called `HelloWorld.cs`. Open a terminal or command prompt and navigate to the directory where the file is located. Use the following command to compile and run the program:
                        
            ```
            csc HelloWorld.cs
            HelloWorld
            ```
                        
            You should see the output `Hello, World!` printed to the console. Let me know if you have any other questions!"),
            		},
            		Temperature = (float)0.7,
            		MaxTokens = 800,
            		NucleusSamplingFactor = (float)0.95,
            		FrequencyPenalty = 0,
            		PresencePenalty = 0,
            	});
            using StreamingChatCompletions streamingChatCompletions = response.Value;
                        
                        
            // ### If streaming is not selected
            Response<ChatCompletions> responseWithoutStream = await client.GetChatCompletionsAsync(
            	"deployment-230822104637",
            	new ChatCompletionsOptions()
            	{
            		Messages =
            		{
            			new ChatMessage(ChatRole.System, @"You are a java expert."),
            			new ChatMessage(ChatRole.User, @"hello, world in java."),
            			new ChatMessage(ChatRole.Assistant, @"Hello! Here's the \\"Hello, World!\\" program in Java:
                        
            ```java
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println(\\"Hello, World!\\");
                }
            }
            ```
                        
            To run this program, save it to a file called `HelloWorld.java`, then open a terminal or command prompt and navigate to the directory where the file is located. Use the following command to compile and run the program:
                        
            ```
            javac HelloWorld.java
            java HelloWorld
            ```
                        
            You should see the output `Hello, World!` printed to the console. Let me know if you have any further questions!"),
            			new ChatMessage(ChatRole.User, @"hello, world in c#"),
            			new ChatMessage(ChatRole.Assistant, @"Certainly! Here's the \\"Hello, World!\\" program in C#:
                        
            ```csharp
            using System;
                        
            class HelloWorld {
                static void Main() {
                    Console.WriteLine(\\"Hello, World!\\");
                }
            }
            ```
                        
            To run this program, save it to a file called `HelloWorld.cs`. Open a terminal or command prompt and navigate to the directory where the file is located. Use the following command to compile and run the program:
                        
            ```
            csc HelloWorld.cs
            HelloWorld
            ```
                        
            You should see the output `Hello, World!` printed to the console. Let me know if you have any other questions!"),
            		},
            		Temperature = (float)0.7,
            		MaxTokens = 800,
            		NucleusSamplingFactor = (float)0.95,
            		FrequencyPenalty = 0,
            		PresencePenalty = 0,
            	});
                        
            ChatCompletions completions = responseWithoutStream.Value;
            """;
    }
}
