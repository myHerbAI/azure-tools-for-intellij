/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ExamplePanel implements AzureForm<SystemMessage.Example> {
    @Getter
    private JPanel panel;
    private JLabel lblDeleteIcon;
    private JTextArea areaUser;
    private JTextArea areaAssistant;

    public ExamplePanel() {
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.lblDeleteIcon.setIcon(AllIcons.General.Remove);
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
        return Collections.emptyList();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.areaUser = new JBTextArea();
        this.areaUser.setBackground(JBColor.background());
        this.areaUser.setBorder(JBUI.Borders.customLine(JBColor.border().brighter()));
        this.areaAssistant = new JBTextArea();
        this.areaAssistant.setBackground(JBColor.background());
        this.areaAssistant.setBorder(JBUI.Borders.customLine(JBColor.border().brighter()));
    }

    private void addRemoveListener(@Nonnull final Consumer<MouseEvent> listener) {
        Arrays.stream(this.lblDeleteIcon.getMouseListeners()).forEach(lblDeleteIcon::removeMouseListener);
        this.lblDeleteIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                listener.accept(e);
            }
        });
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
