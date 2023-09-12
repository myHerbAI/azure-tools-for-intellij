/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.input;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks.CreateCognitiveDeploymentTask.DEFAULT_COGNITIVE_DEPLOYMENT;
import static com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks.SelectSubscriptionTask.DEFAULT_SUBSCRIPTION;

public class CognitiveSubscriptionInput implements GuidanceInput {
    public static final String SUBSCRIPTION = "cognitive_subscription";
    private final InputConfig config;
    private final ComponentContext context;

    private final CognitiveSubscriptionInputPanel panel;

    public CognitiveSubscriptionInput(InputConfig config, ComponentContext context) {
        this.config = config;
        this.context = context;
        this.panel = new CognitiveSubscriptionInputPanel();

        this.panel.setValue((Subscription) context.getParameter(DEFAULT_SUBSCRIPTION));
        context.addPropertyListener(DEFAULT_SUBSCRIPTION, id -> panel.setValue((Subscription) id));
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Nonnull
    @Override
    public AzureFormJPanel getComponent() {
        return this.panel;
    }

    @Override
    public void applyResult() {
        Optional.ofNullable(panel.getValue()).ifPresent(subscription -> context.applyResult(SUBSCRIPTION, subscription));
    }
}
