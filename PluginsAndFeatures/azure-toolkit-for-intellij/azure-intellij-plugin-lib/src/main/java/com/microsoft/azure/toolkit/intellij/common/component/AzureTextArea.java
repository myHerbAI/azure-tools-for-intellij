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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import java.util.concurrent.CopyOnWriteArrayList;

public class AzureTextArea extends JBTextArea implements AzureFormInputComponent<String> {
    public static final Border DEFAULT_BORDER = JBUI.Borders.compound(JBUI.Borders.customLine(new JBColor(12895428, 6185056), 1), JBUI.Borders.empty(6));
    private static final int DEBOUNCE_DELAY = 200;
    private final TailingDebouncer debouncer = new TailingDebouncer(this::fireValueChangedEvent, DEBOUNCE_DELAY);
    @Getter
    final CopyOnWriteArrayList<AzureValueChangeListener<String>> valueChangedListeners = new CopyOnWriteArrayList<>();

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
        this.setBorder(DEFAULT_BORDER);
    }

    @Override
    public String getValue() {
        return this.getText();
    }

    @Override
    public void setValue(String val) {
        this.setText(val);
    }

    public void addValueChangedListener(AzureValueChangeListener<String> listener) {
        if (!valueChangedListeners.contains(listener)) {
            valueChangedListeners.add(listener);
        }
    }

    public void removeValueChangedListener(AzureValueChangeListener<String> listener) {
        valueChangedListeners.remove(listener);
    }
}
