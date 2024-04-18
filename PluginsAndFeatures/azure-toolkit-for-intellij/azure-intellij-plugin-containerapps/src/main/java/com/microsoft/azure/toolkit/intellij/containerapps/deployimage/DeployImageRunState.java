/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.ContainerService;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class DeployImageRunState extends AzureRunProfileState<ContainerApp> {
    private final DeployImageModel dataModel;
    private final DeployImageRunConfiguration configuration;

    public DeployImageRunState(Project project, DeployImageRunConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.dataModel = configuration.getDataModel();
    }

    @Override
    @AzureOperation(name = "platform/containerapps.deploy_image.app", params = {"nameFromResourceId(this.dataModel.getContainerAppId())"})
    public ContainerApp executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        if (!dataModel.isRemoteBuild()) {
            final DockerImage image = configuration.getDockerImageConfiguration();
            ContainerService.getInstance().pushDockerImage(configuration);
        }
        // update Image
        final String containerAppId = dataModel.getContainerAppId();
        final ContainerApp containerApp = Objects.requireNonNull(Azure.az(AzureContainerApps.class).getById(containerAppId), String.format("Container app %s was not found", dataModel.getContainerAppId()));
        final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(dataModel.getContainerRegistryId());
        final Module module = Arrays.stream(ModuleManager.getInstance(project).getModules())
                .filter(m -> StringUtils.equalsAnyIgnoreCase(m.getName(), dataModel.getModuleName()))
                .findFirst().orElse(null);
        if (Objects.nonNull(module)) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            final AzureModule azureModule = AzureModule.from(module);
            tm.runLater(() -> tm.write(() -> {
                final Profile p = azureModule.initializeWithDefaultProfileIfNot();
                Optional.ofNullable(registry).ifPresent(p::addApp);
                Optional.of(containerApp).ifPresent(p::addApp);
                p.save();
            }));
        }
        final ContainerAppDraft draft = (ContainerAppDraft) containerApp.update();
        draft.setConfig(dataModel.getContainerAppConfig());
        draft.updateIfExist();
        return containerApp;
    }

    @Nonnull
    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.ACR, TelemetryConstants.ACR_PUSHIMAGE);
    }

    @Override
    protected void onSuccess(@Nonnull final ContainerApp app, @Nonnull RunProcessHandler processHandler) {
        final String image = Optional.ofNullable(configuration.getDockerImageConfiguration())
                .map(DockerImage::getImageName)
                .map(name -> String.format("Image (%s)", name))
                .orElse("Image");
        processHandler.setText(String.format("%s has been deployed to Container App (%s).", image, app.getName()));
        if (app.isIngressEnabled()) {
            processHandler.setText(String.format("URL: https://%s", app.getIngressFqdn()));
        }
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        return Collections.emptyMap();
    }
}
