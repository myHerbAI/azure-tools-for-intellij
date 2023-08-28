package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class ViewCodeDialog extends DialogWrapper {
    private final ChatBot chatBot;
    private JPanel contentPane;
    private ViewCodePanel viewCodePanel;

    public ViewCodeDialog(@Nullable final Project project, final ChatBot chatBot) {
        super(project);
        this.chatBot = chatBot;
        this.init();
        this.setTitle("Sample Code");
        this.setOKButtonText("Copy");
        this.setSize(520, 550);
        this.setResizable(true);
    }

    @Override
    protected void doOKAction() {
        final String code = this.viewCodePanel.getCode();
        super.doOKAction();
        AzureTaskManager.getInstance().runLater(() -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
            AzureMessager.getMessager().success("Sample code is copied to clipboard.");
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    private void createUIComponents() {
        this.viewCodePanel = new ViewCodePanel(this.chatBot);
    }
}
