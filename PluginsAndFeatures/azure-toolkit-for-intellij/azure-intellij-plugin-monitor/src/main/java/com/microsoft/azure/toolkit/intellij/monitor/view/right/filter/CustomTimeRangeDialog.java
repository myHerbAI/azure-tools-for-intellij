/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.versionBrowser.DateFilterComponent;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.vcs.log.VcsLogDateFilter;
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomTimeRangeDialog extends AzureDialog<String> implements AzureForm<String> {
    private final DateFilterComponent dateFilterComponent;
    private String customKustoString;
    public static final String CUSTOM_BEFORE = "AzureMonitor.Custom.Before";
    public static final String CUSTOM_AFTER = "AzureMonitor.Custom.After";

    public CustomTimeRangeDialog() {
        super();
        this.dateFilterComponent = new DateFilterComponent(false, DateFormatUtil.getDateFormat().getDelegate());
        restoreDate();
        init();
    }
    @Override
    public AzureForm<String> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Select Time Range";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.dateFilterComponent.getPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return this.dateFilterComponent.getPanel();
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final List<ValidationInfo> res = new ArrayList<>();
        Optional.ofNullable(dateFilterComponent.validateInput()).ifPresent(s -> res.add(new ValidationInfo(s).asWarning()));
        if (dateFilterComponent.getBefore() < 0 && dateFilterComponent.getAfter() < 0) {
            res.add(new ValidationInfo("Need to set at least one time.").asWarning());
        }
        if (dateFilterComponent.getBefore() < dateFilterComponent.getAfter()) {
            res.add(new ValidationInfo("Before date should be larger or equal to after date.").asWarning());
        }
        return res;
    }

    @Override
    protected void doOKAction() {
        PropertiesComponent.getInstance().setValue(CUSTOM_AFTER, String.valueOf(dateFilterComponent.getAfter()));
        PropertiesComponent.getInstance().setValue(CUSTOM_BEFORE, String.valueOf(dateFilterComponent.getBefore()));
        setCustomKustoString();
        super.doOKAction();
    }


    @Override
    public String getValue() {
        return customKustoString;
    }

    @Override
    public void setValue(String data) {
        this.customKustoString = data;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return new ArrayList<>();
    }

    private void restoreDate() {
        Optional.ofNullable(PropertiesComponent.getInstance().getValue(CUSTOM_AFTER)).ifPresent(t -> this.dateFilterComponent.setAfter(Long.parseLong(t)));
        Optional.ofNullable(PropertiesComponent.getInstance().getValue(CUSTOM_BEFORE)).ifPresent(t -> this.dateFilterComponent.setBefore(Long.parseLong(t)));
        setCustomKustoString();
    }

    private void setCustomKustoString() {
        final VcsLogDateFilter filter = VcsLogFilterObject.fromDates(dateFilterComponent.getAfter(), dateFilterComponent.getBefore());
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        final String kustoAfter = Optional.ofNullable(filter.getAfter())
                .map(d -> String.format("where TimeGenerated >= datetime(%s)", formatter.format(d))).orElse(StringUtils.EMPTY);
        final String kustoBefore = Optional.ofNullable(filter.getBefore())
                .map(d -> String.format("where TimeGenerated <= datetime(%s)", formatter.format(d))).orElse(StringUtils.EMPTY);
        customKustoString = StringUtils.join(new String[] {kustoBefore, kustoAfter}, " | ");
    }

}
