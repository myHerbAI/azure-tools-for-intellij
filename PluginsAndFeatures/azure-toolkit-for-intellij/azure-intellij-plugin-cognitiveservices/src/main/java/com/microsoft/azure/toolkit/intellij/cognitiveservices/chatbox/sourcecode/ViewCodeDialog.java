package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsContexts;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ViewCodeDialog extends DialogWrapper {
    private final ChatBot chatBot;
    private JPanel contentPane;
    private ViewCodePanel viewCodePanel;

    public ViewCodeDialog(@Nullable final Project project, final ChatBot chatBot) {
        super(project);
        this.chatBot = chatBot;
        this.init();
        this.setSize(500, 550);
        this.setResizable(true);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    @Override
    public @NlsContexts.DialogTitle String getTitle() {
        return "Sample Code";
    }

    private void createUIComponents() {
        this.viewCodePanel = new ViewCodePanel(this.chatBot);
    }
}
