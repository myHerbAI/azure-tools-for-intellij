package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;

public interface ISourceCodeGenerator {
    String getName();
    String getLanguage();
    String generateCode(ChatBot chatBot);
}
