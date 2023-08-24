/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;

import javax.annotation.Nonnull;
import java.util.Objects;

public class OpenPlaygroundTask implements Task {
    private ComponentContext context;

    public OpenPlaygroundTask(ComponentContext taskContext) {
        this.context = taskContext;
    }

    @Override
    public void execute() {
        final CognitiveDeployment deployment = (CognitiveDeployment)
                Objects.requireNonNull(context.getParameter(CreateCognitiveDeploymentTask.DEPLOYMENT), "Deployment cannot be null");
        IntellijShowPropertiesViewAction.showPropertyView(deployment, context.getProject());
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.open_ai.open_playground";
    }
}
