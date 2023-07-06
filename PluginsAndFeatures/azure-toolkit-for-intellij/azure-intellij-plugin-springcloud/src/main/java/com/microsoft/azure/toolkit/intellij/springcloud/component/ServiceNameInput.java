package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.azure.core.management.Region;
import com.azure.resourcemanager.appplatform.models.NameAvailability;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ServiceNameInput extends AzureTextInput {
    private Subscription subscription;
    private static final String NAME_REGEX = "^[a-z][a-z0-9-]*[a-z0-9]$";
    private Region region;

    public ServiceNameInput() {
        super();
        this.addValidator(this::doValidateValue);
    }

    public void setRegion(Region region) {
        if (!Objects.equals(region, this.region)) {
            this.region = region;
            this.validateValueAsync();
        }
    }

    public void setSubscription(Subscription subscription) {
        if (!Objects.equals(subscription, this.subscription)) {
            this.subscription = subscription;
            this.validateValueAsync();
        }
    }

    @Nonnull
    public AzureValidationInfo doValidateValue() {
        final String name = this.getValue();
        if (this.subscription != null && this.isEnabled()) {
            try {
                if (name.length() < 4 || name.length() > 32) {
                    throw new IllegalArgumentException("It must be between 4 and 32 characters long.");
                }
                if (!name.matches(NAME_REGEX)) {
                    throw new IllegalArgumentException("It can contain only lowercase letters, numbers and hyphens and the first character must be a letter but the last character can be a letter or number.");
                }
                final NameAvailability result = Azure.az(AzureSpringCloud.class).forSubscription(this.subscription.getId()).checkNameAvailability(this.region, name);
                if (!result.nameAvailable()) {
                    throw new IllegalArgumentException(result.reason());
                }
            } catch (final IllegalArgumentException e) {
                return AzureValidationInfo.error(e.getMessage(), this);
            }
            return AzureValidationInfo.success(this);
        } else {
            return AzureValidationInfo.none(this);
        }
    }
}
