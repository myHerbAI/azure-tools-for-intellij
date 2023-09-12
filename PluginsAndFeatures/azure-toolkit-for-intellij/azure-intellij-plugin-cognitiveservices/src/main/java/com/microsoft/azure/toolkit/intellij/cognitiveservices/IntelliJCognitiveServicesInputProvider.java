/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInputProvider;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.input.CognitiveDeploymentInput;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.input.CognitiveSubscriptionInput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IntelliJCognitiveServicesInputProvider implements GuidanceInputProvider {
    @Nullable
    @Override
    public GuidanceInput createInputComponent(@Nonnull InputConfig config, @Nonnull Context context) {
        final ComponentContext inputContext = new ComponentContext(config, context);
        switch (config.getName()) {
            case "input.openai.subscription":
                return new CognitiveSubscriptionInput(config, inputContext);
            case "input.openai.deployment":
                return new CognitiveDeploymentInput(config, inputContext);
            default:
                return null;
        }
    }
}
