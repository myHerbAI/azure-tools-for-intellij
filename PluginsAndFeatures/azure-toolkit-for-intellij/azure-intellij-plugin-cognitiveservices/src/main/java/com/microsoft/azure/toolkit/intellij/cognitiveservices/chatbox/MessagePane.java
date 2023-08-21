/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;

public class MessagePane {
    @Getter
    private JPanel contentPanel;
    private JPanel messageContainer;
    private MarkdownPane markdownPane;

    public MessagePane(String markdown) {
        this.setValue(new MarkdownText(markdown));
    }

    public MessagePane(MarkdownText markdown) {
        final JBColor borderColor = new JBColor(12895428, 6185056);
        final CompoundBorder border = BorderFactory.createCompoundBorder(this.messageContainer.getBorder(), BorderFactory.createLineBorder(borderColor, 1, true));
        this.messageContainer.setBorder(border);
        this.setValue(markdown);
    }

    private void setValue(MarkdownText markdownText) {
        this.markdownPane.setValue(markdownText);
        ApplicationManager.getApplication().invokeLater(() -> {
            this.contentPanel.revalidate();
            this.contentPanel.repaint();
        });
    }
}
