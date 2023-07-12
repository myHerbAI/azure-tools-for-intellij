/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.CheckableRunConfigurationEditor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudDeploymentConfiguration extends LocatableConfigurationBase<Element> implements LocatableConfiguration {
    private static final String NEED_SPECIFY_ARTIFACT = "Please select an artifact";
    private static final String NEED_SPECIFY_SUBSCRIPTION = "Please select your subscription.";
    private static final String NEED_SPECIFY_CLUSTER = "Please select a target cluster.";
    private static final String NEED_SPECIFY_APP_NAME = "Please select a target app.";
    private static final String SERVICE_IS_NOT_READY = "Spring Apps is not ready for deploy, current status is ";
    private static final String TARGET_CLUSTER_DOES_NOT_EXISTS = "Target cluster does not exists.";
    private static final String TARGET_CLUSTER_IS_NOT_AVAILABLE = "Target cluster cannot be found in current subscription";

    @Getter
    @Nullable
    private SpringCloudDeploymentDraft deployment;
    private ResourceId appId;
    private WrappedAzureArtifact artifact;

    public SpringCloudDeploymentConfiguration(@Nonnull Project project, @Nonnull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.getProject());
        this.appId = Optional.ofNullable(element.getChild("App"))
            .map(e -> e.getAttributeValue("id"))
            .filter(StringUtils::isNotBlank)
            .map(ResourceId::fromString)
            .orElse(null);
        if (this.appId == null) {
            final SpringCloudAppConfig appConfig = Optional.ofNullable(element.getChild("SpringCloudAppConfig"))
                .map(e -> XmlSerializer.deserialize(e, SpringCloudAppConfig.class))
                .orElse(SpringCloudAppConfig.builder().deployment(SpringCloudDeploymentConfig.builder().build()).build());
            if (Objects.nonNull(appConfig)) {
                final String appId = String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.AppPlatform/Spring/%s/apps/%s",
                    appConfig.getSubscriptionId(), appConfig.getResourceGroup(), appConfig.getClusterName(), appConfig.getAppName());
                this.appId = ResourceId.fromString(appId);
            }
        }
        final String artifactId = Optional.ofNullable(element.getChild("Artifact"))
            .map(e -> e.getAttributeValue("identifier")).orElse(null);
        this.artifact = Optional.ofNullable(artifactId)
            .map(manager::getAzureArtifactById)
            .or(() -> Optional.ofNullable(AzureArtifact.createFromFile(artifactId, this.getProject())))
            .map(a -> new WrappedAzureArtifact(a, this.getProject()))
            .orElse(null);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        Optional.ofNullable(this.appId)
            .map(id -> new Element("App").setAttribute("id", id.id()))
            .ifPresent(element::addContent);
        Optional.ofNullable(this.artifact)
            .map((a) -> a.getArtifact().getIdentifier())
            .map(id -> new Element("Artifact").setAttribute("identifier", id))
            .ifPresent(element::addContent);
    }

    public void setDeployment(@Nullable SpringCloudDeploymentDraft deployment) {
        this.deployment = deployment;
        if (Objects.nonNull(deployment)) {
            this.appId = ResourceId.fromString(deployment.getParent().getId());
            this.artifact = (WrappedAzureArtifact) deployment.getArtifact();
        }
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new Editor(this, getProject());
    }

    @Nullable
    @Override
    @ExceptionNotification
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment executionEnvironment) {
        restoreDeployment(this);
        return new SpringCloudDeploymentConfigurationState(getProject(), this);
    }

    @Override
    public void checkConfiguration() {
    }

    static class Factory extends ConfigurationFactory {
        public static final String FACTORY_NAME = "Deploy Spring app";

        public Factory(@Nonnull ConfigurationType type) {
            super(type);
        }

        @Nonnull
        @Override
        public RunConfiguration createTemplateConfiguration(@Nonnull Project project) {
            return new SpringCloudDeploymentConfiguration(project, this, project.getName());
        }

        @Override
        public RunConfiguration createConfiguration(String name, RunConfiguration template) {
            return new SpringCloudDeploymentConfiguration(template.getProject(), this, name);
        }

        @Override
        public @Nonnull String getId() {
            return "Deploy Spring Cloud Services";
        }

        @Override
        public String getName() {
            return FACTORY_NAME;
        }
    }

    static class Editor extends SettingsEditor<SpringCloudDeploymentConfiguration> implements CheckableRunConfigurationEditor<SpringCloudDeploymentConfiguration> {
        private final SpringCloudDeploymentConfigurationPanel panel;

        Editor(SpringCloudDeploymentConfiguration configuration, Project project) {
            super();
            this.panel = new SpringCloudDeploymentConfigurationPanel(configuration, project);
        }

        @Override
        @ExceptionNotification
        protected void resetEditorFrom(@Nonnull SpringCloudDeploymentConfiguration config) {
            this.panel.setConfiguration(config);
            restoreDeployment(config);
            if (Objects.nonNull(config.deployment)) {
                AzureTaskManager.getInstance().runLater(() -> this.panel.setValue(config.deployment), AzureTask.Modality.ANY);
            }
        }

        @Override
        protected void applyEditorTo(@Nonnull SpringCloudDeploymentConfiguration config) throws ConfigurationException {
            final List<AzureValidationInfo> infos = this.panel.getAllValidationInfos(true);
            final AzureValidationInfo error = infos.stream()
                .filter(i -> !i.isValid())
                .findAny().orElse(null);
            if (Objects.nonNull(error)) {
                final String message = error.getType() == Type.PENDING ? "Please try later after validation" : error.getMessage();
                throw new ConfigurationException(message);
            }
            config.setDeployment(this.panel.getValue());
        }

        @Override
        protected @Nonnull JComponent createEditor() {
            return this.panel.getContentPanel();
        }

        @Override
        public void checkEditorData(SpringCloudDeploymentConfiguration s) {
        }
    }

    private static void restoreDeployment(@Nonnull SpringCloudDeploymentConfiguration config) {
        if (config.deployment == null && Objects.nonNull(config.appId)) {
            final SpringCloudApp app = Azure.az(AzureSpringCloud.class).getById(config.appId.id());
            if (Objects.nonNull(app)) {
                final SpringCloudDeployment d = Optional.ofNullable(app.getActiveDeployment()).orElseGet(() -> app.deployments().create("default", null));
                config.deployment = (SpringCloudDeploymentDraft) (d.isDraft() ? d : d.update());
                config.deployment.setArtifact(config.artifact);
            }
        }
    }
}
