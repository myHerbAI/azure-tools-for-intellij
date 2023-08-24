package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import lombok.Getter;

@Getter
public class JsonSourceCodeGenerator implements ISourceCodeGenerator {
    private final String name = "JSON";
    private final String language = "json";

    @Override
    public String generateCode(final ChatBot chatBot) {
        return """
            {
              "messages": [
                {
                  "role": "system",
                  "content": "You are a java expert."
                },
                {
                  "role": "user",
                  "content": "hello, world in java."
                },
                {
                  "role": "assistant",
                  "content": "Hello! Here's the \\"Hello, World!\\" program in Java:\\n\\n```java\\npublic class HelloWorld {\\n    public static void main(String[] args) {\\n        System.out.println(\\"Hello, World!\\");\\n    }\\n}\\n```\\n\\nTo run this program, save it to a file called `HelloWorld.java`, then open a terminal or command prompt and navigate to the directory where the file is located. Use the following command to compile and run the program:\\n\\n```\\njavac HelloWorld.java\\njava HelloWorld\\n```\\n\\nYou should see the output `Hello, World!` printed to the console. Let me know if you have any further questions!"
                },
                {
                  "role": "user",
                  "content": "hello, world in c#"
                },
                {
                  "role": "assistant",
                  "content": "Certainly! Here's the \\"Hello, World!\\" program in C#:\\n\\n```csharp\\nusing System;\\n\\nclass HelloWorld {\\n    static void Main() {\\n        Console.WriteLine(\\"Hello, World!\\");\\n    }\\n}\\n```\\n\\nTo run this program, save it to a file called `HelloWorld.cs`. Open a terminal or command prompt and navigate to the directory where the file is located. Use the following command to compile and run the program:\\n\\n```\\ncsc HelloWorld.cs\\nHelloWorld\\n```\\n\\nYou should see the output `Hello, World!` printed to the console. Let me know if you have any other questions!"
                }
              ],
              "temperature": 0.7,
              "top_p": 0.95,
              "frequency_penalty": 0,
              "presence_penalty": 0,
              "max_tokens": 800,
              "stop": null
            }
            """;
    }
}
