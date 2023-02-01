/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TimeRangeComboBox extends AzureComboBox<TimeRangeComboBox.TimeRange> {
    private String customKustoString;
    private boolean isClicked = false;
    public TimeRangeComboBox() {
        super();
        final Component component = this.getEditor().getEditorComponent();
        if (component instanceof JTextField) {
            ((JTextField) component).getDocument().addDocumentListener((TextDocumentListenerAdapter) () -> {
                final TimeRange timeRange = (TimeRange) getSelectedItem();
                final String selectedText = Optional.ofNullable(timeRange).map(TimeRange::getLabel).orElse(StringUtils.EMPTY);
                final String text = ((JTextField) component).getText();
                if (isClicked && Objects.equals(TimeRange.CUSTOM.label, text)) {
                    final CustomTimeRangeDialog dialog = new CustomTimeRangeDialog();
                    if (dialog.showAndGet()) {
                        customKustoString = dialog.getValue();
                    }
                }
                isClicked = !Objects.equals(selectedText, text);
            });
        }
    }

    public String getKustoString() {
        final TimeRange selectedTimeRange = this.getValue();
        if (TimeRange.CUSTOM.equals(selectedTimeRange)) {
            return this.customKustoString;
        }
        if (Objects.nonNull(selectedTimeRange)) {
            return selectedTimeRange.getKustoString();
        }
        return TimeRange.LAST_24_HOURS.kustoString;
    }

    @Nonnull
    @Override
    protected List<? extends TimeRange> loadItems() {
        return Arrays.asList(
                TimeRange.LAST_30_MINUTES,
                TimeRange.LAST_HOUR,
                TimeRange.LAST_4_HOURS,
                TimeRange.LAST_12_HOURS,
                TimeRange.LAST_24_HOURS,
                TimeRange.LAST_48_HOURS,
                TimeRange.LAST_3_DAYS,
                TimeRange.LAST_7_DAYS,
                TimeRange.CUSTOM
        );
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((TimeRange) item).label;
    }

    @Override
    public TimeRange getDefaultValue() {
        if (Objects.isNull(doGetDefaultValue())) {
            return TimeRange.LAST_24_HOURS;
        }
        return super.getDefaultValue();
    }

    public static class TimeRange {
        public static final TimeRange LAST_30_MINUTES = new TimeRange("Last 30 minutes", "where TimeGenerated > ago(30m)");
        public static final TimeRange LAST_HOUR = new TimeRange("Last hour", "where TimeGenerated > ago(1h)");
        public static final TimeRange LAST_4_HOURS = new TimeRange("Last 4 hours", "where TimeGenerated > ago(4h)");
        public static final TimeRange LAST_12_HOURS = new TimeRange("Last 12 hours", "where TimeGenerated > ago(12h)");
        public static final TimeRange LAST_24_HOURS = new TimeRange("Last 24 hours", "where TimeGenerated > ago(24h)");
        public static final TimeRange LAST_48_HOURS = new TimeRange("Last 48 hours", "where TimeGenerated > ago(48h)");
        public static final TimeRange LAST_3_DAYS = new TimeRange("Last 3 days", "where TimeGenerated > ago(3d)");
        public static final TimeRange LAST_7_DAYS = new TimeRange("Last 7 days", "where TimeGenerated > ago(7d)");
        public static final TimeRange CUSTOM = new TimeRange("Custom", "");
        @Nonnull
        @Getter
        private final String label;
        @Nonnull
        @Getter
        private final String kustoString;
        public TimeRange(@Nonnull String label, @Nonnull String kustoString) {
            this.label = label;
            this.kustoString = kustoString;
        }
    }
}
