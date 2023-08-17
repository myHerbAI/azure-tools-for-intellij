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

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks.CreateCognitiveDeploymentTask.DEFAULT_COGNITIVE_DEPLOYMENT;


public class CognitiveDeploymentInput implements GuidanceInput {
    public static final String COGNITIVE_DEPLOYMENT = "cognitive_deployment";
    public static final String COGNITIVE_ACCOUNT = "cognitive_account";
    private final InputConfig config;
    private final ComponentContext context;
    private final CognitiveDeploymentInputPanel panel;

    public CognitiveDeploymentInput(InputConfig config, ComponentContext inputContext) {
        this.config = config;
        this.context = inputContext;
        this.panel = new CognitiveDeploymentInputPanel();

        this.panel.setValue((CognitiveDeployment) context.getParameter(DEFAULT_COGNITIVE_DEPLOYMENT));
        context.addPropertyListener(DEFAULT_COGNITIVE_DEPLOYMENT, id -> panel.setValue((CognitiveDeployment) id));
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Nonnull
    @Override
    public AzureFormJPanel getComponent() {
        return panel;
    }

    @Override
    public void applyResult() {
        Optional.ofNullable(panel.getValue()).ifPresent(deployment -> context.applyResult(COGNITIVE_DEPLOYMENT, deployment));
        Optional.ofNullable(panel.getValue()).map(CognitiveDeployment::getParent).ifPresent(account -> context.applyResult(COGNITIVE_ACCOUNT, account));
    }
}
