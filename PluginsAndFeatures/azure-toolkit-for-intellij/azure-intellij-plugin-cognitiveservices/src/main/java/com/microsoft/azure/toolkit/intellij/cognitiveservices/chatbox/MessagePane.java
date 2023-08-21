/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.intellij.collaboration.ui.codereview.comment.RoundedPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;

import javax.swing.*;

public class MessagePane {
    public static final JBColor BACKGROUND_COLOR =
        JBColor.namedColor("StatusBar.hoverBackground", new JBColor(15595004, 4606541));
    @Getter
    private JPanel contentPanel;
    private JPanel messageContainer;
    private MarkdownPane markdownPane;

    public MessagePane(String markdown) {
        this.setValue(new MarkdownText(markdown));
    }

    public MessagePane(MarkdownText markdown) {
//        final JBColor borderColor = new JBColor(12895428, 6185056);
//        final CompoundBorder border = BorderFactory.createCompoundBorder(this.messageContainer.getBorder(), BorderFactory.createLineBorder(borderColor, 1, true));
//        this.messageContainer.setBorder(border);
        this.setValue(markdown);
    }

    private void setValue(MarkdownText markdownText) {
        this.markdownPane.setValue(markdownText);
        ApplicationManager.getApplication().invokeLater(() -> {
            this.contentPanel.revalidate();
            this.contentPanel.repaint();
        });
    }

    private void createUIComponents() {
        final int arc = NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get();
        //noinspection UnstableApiUsage
        this.messageContainer = new RoundedPanel(new GridLayoutManager(1, 1), 5);
        this.messageContainer.setBackground(BACKGROUND_COLOR);
        this.messageContainer.setBorder(BorderFactory.createEmptyBorder());
    }
}
