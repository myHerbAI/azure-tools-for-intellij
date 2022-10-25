/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common.component.resourcegroup;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class ResourceGroupNameTextField extends AzureTextInput {

    private static final Pattern PATTERN = Pattern.compile("^[-\\w._()]+$");
    private static final String INVALID_CHARACTERS = "Resource group names only allow alphanumeric characters, periods, underscores, hyphens and parenthesis " +
        "and cannot end in a period.";
    public static final String INVALID_LENGTH = "Resource group names only allow up to 90 characters.";
    public static final String CONFLICT_NAME = "A resource group with the same name already exists in the selected subscription %s";
    private Subscription subscription;

    public ResourceGroupNameTextField() {
        super();
        this.addValidator(this::doValidateValue);
        this.setRequired(true);
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        // UI dialog will call set subscription after shown, which may trigger validation and show unnecessary error message
        // so skip validation if validation info is not set, we just need to revalidate after change to another subscription
        if (this.getValidationInfo() != null) {
            this.validateValueAsync();
        }
    }

    public AzureValidationInfo doValidateValue() {
        final String value = this.getValue();
        // validate length
        final int maxLength = 90;
        if (StringUtils.length(value) < 1) {
            return AzureValidationInfo.error("The value must not be empty.", this);
        } else if (StringUtils.length(value) > maxLength) {
            return AzureValidationInfo.error(INVALID_LENGTH, this);
        }
        // validate special character
        if (value.endsWith(".") || !PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.error(INVALID_CHARACTERS, this);
        }
        // validate availability
        try {
            if (Azure.az(AzureResources.class).groups(subscription.getId()).exists(value)) {
                return AzureValidationInfo.error(String.format(CONFLICT_NAME, subscription.getName()), this);
            }
        } catch (final Exception e) {
            return AzureValidationInfo.error(e.getMessage(), this);
        }
        return AzureValidationInfo.success(this);
    }
}
