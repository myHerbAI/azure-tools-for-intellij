/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class KustoFilterComboBox extends AzureComboBox<String> {
    private String kustoString;
    @Setter
    private String columnName;
    public final static String ALL = "All";
    public KustoFilterComboBox() {
        super();
    }

    public String getKustoString() {
        final String selected = this.getValue();
        if (Objects.isNull(selected) || Objects.equals(ALL, selected)) {
            return StringUtils.EMPTY;
        }
        return String.format("where %s == \"%s\"", columnName, selected);
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        try {
            return ResourceId.fromString(item.toString()).name();
        } catch (final Exception e) {
            return item.toString();
        }
    }

    @Override
    public String getDefaultValue() {
        if (Objects.isNull(doGetDefaultValue())) {
            return ALL;
        }
        return super.getDefaultValue();
    }
}
