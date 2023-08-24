/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import org.jetbrains.annotations.NotNull;

import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;

public class AzureTextArea extends JBTextArea implements AzureFormInputComponent<String> {
    private static final int DEBOUNCE_DELAY = 200;
    private final TailingDebouncer debouncer = new TailingDebouncer(this::fireValueChangedEvent, DEBOUNCE_DELAY);

    public AzureTextArea() {
        super();
        this.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                validateValueAsync();
                debouncer.debounce();
            }
        });
        final JBColor borderColor = new JBColor(12895428, 6185056);
        this.setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.setLineWrap(true);
        this.setFont(JBUI.Fonts.label());
        final Border border = JBUI.Borders.compound(JBUI.Borders.customLine(borderColor, 1), JBUI.Borders.empty(6));
        this.setBorder(border);
    }

    @Override
    public String getValue() {
        return this.getText();
    }

    @Override
    public void setValue(String val) {
        this.setText(val);
    }
}
