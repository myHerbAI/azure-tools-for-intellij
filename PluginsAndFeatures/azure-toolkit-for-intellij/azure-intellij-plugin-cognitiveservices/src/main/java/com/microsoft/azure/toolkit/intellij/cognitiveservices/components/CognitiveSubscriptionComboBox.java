/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

public class CognitiveSubscriptionComboBox extends SubscriptionComboBox {
    @Override
    protected String getItemText(Object item) {
        if (item instanceof Subscription) {
            final Subscription subscription = (Subscription) item;
            final boolean openAIEnabled = Azure.az(AzureAccount.class).isLoggedIn() &&
                    Azure.az(AzureCognitiveServices.class).isOpenAIEnabled((subscription).getId());
            return openAIEnabled ? subscription.getName() : String.format("%s (Not Available)", subscription.getName());
        }
        return super.getItemText(item);
    }
}
