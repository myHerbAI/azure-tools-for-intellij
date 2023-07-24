/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.task;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CreateSpringAppTask implements Task {
    public static final String SPRING_APP_CLUSTER = "springAppCluster";
    public static final String SPRING_APP = "springApp";
    public static final String SPRING_APP_NAME = "springAppName";
    public static final String DEFAULT_SPRING_APP_NAME = "defaultSpringAppName";
    private final ComponentContext context;

    public CreateSpringAppTask(@Nonnull final ComponentContext context) {
        this.context = context;
        this.init();
    }

    @Override
    public void prepare() {
        final Phase currentPhase = context.getCourse().getCurrentPhase();
        currentPhase.expandPhasePanel();
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_spring_app")
    public void execute() {
        final SpringCloudCluster cluster = (SpringCloudCluster) Objects.requireNonNull(context.getParameter(SPRING_APP_CLUSTER), "`cluster` is required to create spring app");
        final String name = (String) Objects.requireNonNull(context.getParameter(SPRING_APP_NAME), "`name` is required to create spring app");
        final SpringCloudAppDraft appDraft = cluster.apps().create(name, null);
        appDraft.setPublicEndpointEnabled(true);
        final SpringCloudDeploymentDraft deploymentDraft = appDraft.deployments().create("default", null);
        deploymentDraft.setRuntimeVersion(RuntimeVersion.JAVA_17.toString());
        deploymentDraft.setCapacity(1);
        deploymentDraft.commit();
        AzureMessager.getMessager().info(AzureString.format("Azure Spring app %s is successfully created.", name));
        context.applyResult(SPRING_APP, appDraft);
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.springcloud.create";
    }

    private void init() {
        final String defaultSpringAppName = Utils.generateRandomResourceName("app", 40);
        context.applyResult(DEFAULT_SPRING_APP_NAME, defaultSpringAppName);
    }
}
