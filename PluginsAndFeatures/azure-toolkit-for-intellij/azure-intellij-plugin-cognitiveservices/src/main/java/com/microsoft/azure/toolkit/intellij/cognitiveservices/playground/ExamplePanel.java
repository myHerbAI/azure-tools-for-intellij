/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.AzureTextArea;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ExamplePanel implements AzureForm<SystemMessage.Example> {
    @Getter
    private JPanel panel;
    private JLabel lblDeleteIcon;
    private JTextArea areaUser;
    private JTextArea areaAssistant;
    private JScrollPane scrollPaneUser;
    private JScrollPane scrollPaneAssistant;
    private JLabel lblUser;
    private JLabel lblAssistant;

    public ExamplePanel() {
        $$$setupUI$$$();
        this.lblDeleteIcon.setIcon(IntelliJAzureIcons.getIcon("/icons/delete-example.svg"));
        areaUser.setBorder(JBUI.Borders.empty());
        scrollPaneUser.setBorder(AzureTextArea.DEFAULT_BORDER);
        areaAssistant.setBorder(JBUI.Borders.empty());
        scrollPaneAssistant.setBorder(AzureTextArea.DEFAULT_BORDER);
        this.lblUser.setBorder(JBUI.Borders.empty(6, 0));
        this.lblAssistant.setBorder(JBUI.Borders.empty(6, 0));
        this.getInputs().forEach(input -> input.addValueChangedListener(ignore -> this.fireValueChangedEvent()));
    }

    public void addDeleteListener(final MouseListener listener) {
        this.lblDeleteIcon.addMouseListener(listener);
    }

    @Override
    public SystemMessage.Example getValue() {
        return SystemMessage.Example.builder().user(areaUser.getText()).assistant(areaAssistant.getText()).build();
    }

    @Override
    public void setValue(@Nullable final SystemMessage.Example data) {
        Optional.ofNullable(data).ifPresent(value -> {
            areaUser.setText(value.getUser());
            areaAssistant.setText(value.getAssistant());
        });
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        // todo: make JTextArea as AzureInput
        return Arrays.asList((AzureFormInput<?>) this.areaUser, (AzureFormInput<?>) this.areaAssistant);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.areaUser = new AzureTextArea();
        this.areaAssistant = new AzureTextArea();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
