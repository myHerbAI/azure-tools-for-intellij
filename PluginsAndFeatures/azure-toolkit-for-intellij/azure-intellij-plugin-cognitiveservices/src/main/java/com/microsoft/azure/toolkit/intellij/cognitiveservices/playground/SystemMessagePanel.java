/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.OpenAISystemTemplateComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SystemMessagePanel implements AzureForm<SystemMessage> {
    public static final String SYSTEM_MESSAGE_DESCRIPTION = "Use a template to get started, or just start writing your own system " +
            "message below. Want some tips? <a href = \"https://go.microsoft.com/fwlink/?linkid=2235756\">Learn more</a>\u2197";
    public static final String EXAMPLE_DESCRIPTION = "Add examples to show the chat what responses you want. " +
            "It will try to mimic any responses you add here so make sure they match the rules you laid out in the system message.";
    private JPanel pnlRoot;
    private JLabel lblSystemTitle;
    private JBLabel lblSystemDescription;
    private JLabel lblTemplate;
    private JLabel lblSystemMessage;
    private JTextArea areaSystemMessage;
    private JBLabel lblExampleDescription;
    private JScrollPane scrollPane;
    private JPanel pnlExample;
    private JButton btnAddExample;
    private OpenAISystemTemplateComboBox cbSystemTemplate;

    private final List<ExamplePanel> panels = new ArrayList<>();

    public SystemMessagePanel() {
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.btnAddExample.setIcon(AllIcons.General.Add);
        this.btnAddExample.addActionListener(this::onAddNewExample);

        this.lblSystemTitle.setFont(JBFont.h3().asBold());

        this.lblSystemDescription.setText(SYSTEM_MESSAGE_DESCRIPTION);
        this.lblSystemDescription.setAllowAutoWrapping(true);
        this.lblSystemDescription.setCopyable(true);// this makes label auto wrapping
        this.lblSystemDescription.setForeground(UIUtil.getContextHelpForeground());

        this.lblExampleDescription.setText(EXAMPLE_DESCRIPTION);
        this.lblExampleDescription.setAllowAutoWrapping(true);
        this.lblExampleDescription.setCopyable(true);// this makes label auto wrapping
        this.lblExampleDescription.setForeground(UIUtil.getContextHelpForeground());

        this.lblSystemMessage.setIcon(AllIcons.General.ContextHelp);
        this.scrollPane.setBorder(JBUI.Borders.empty());
    }

    private void onAddNewExample(final ActionEvent actionEvent) {
        final SystemMessage value = getValue();
        value.getExamples().add(SystemMessage.Example.builder().build());
        setValue(value);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbSystemTemplate = new OpenAISystemTemplateComboBox();

        this.areaSystemMessage = new JBTextArea();
        this.areaSystemMessage.setBackground(JBColor.background());
        this.areaSystemMessage.setBorder(JBUI.Borders.customLine(JBColor.border().brighter()));
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }

    @Override
    public SystemMessage getValue() {
        final SystemMessage.SystemMessageBuilder builder = SystemMessage.builder();
        builder.systemMessage(areaSystemMessage.getText());
        final ArrayList<SystemMessage.Example> examples = new ArrayList<>();
        for (final ExamplePanel panel : panels) {
            examples.add(panel.getValue());
        }
        builder.examples(examples);
        return builder.build();
    }

    @Override
    public void setValue(@Nonnull final SystemMessage data) {
        this.areaSystemMessage.setText(data.getSystemMessage());
        Optional.ofNullable(data.getExamples()).ifPresent(this::renderExamples);
    }

    private void renderExamples(@Nonnull final List<SystemMessage.Example> examples) {
        panels.clear();
        pnlExample.removeAll();
        final GridLayoutManager layout = ((GridLayoutManager) this.pnlExample.getLayout());
        final GridLayoutManager newLayout = new GridLayoutManager(examples.size() + 1, 1, layout.getMargin(), -1, -1);
        this.pnlExample.setLayout(newLayout);
        for (int i = 0; i < examples.size(); i++) {
            final SystemMessage.Example example = examples.get(i);
            final ExamplePanel examplePanel = new ExamplePanel();
            examplePanel.setValue(example);
            final GridConstraints c = new GridConstraints(i, 0, 1, 1, 1, 1, 3, 3, null, null, null, 0, false);
            this.pnlExample.add(examplePanel.getPanel(), c);
            panels.add(examplePanel);
        }
        final GridConstraints c = new GridConstraints(examples.size(), 0, 1, 1, 1, 2, 1, 6, null, null, null, 0, false);
        final Spacer spacer = new Spacer();
        this.pnlExample.add(spacer, c);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.emptyList();
    }
}
