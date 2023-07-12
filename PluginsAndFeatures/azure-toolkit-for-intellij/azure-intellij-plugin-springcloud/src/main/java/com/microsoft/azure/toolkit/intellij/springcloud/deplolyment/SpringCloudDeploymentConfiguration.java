/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.CheckableRunConfigurationEditor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public class SpringCloudDeploymentConfiguration extends LocatableConfigurationBase<Element> implements LocatableConfiguration {
    private static final String NEED_SPECIFY_ARTIFACT = "Please select an artifact";
    private static final String NEED_SPECIFY_SUBSCRIPTION = "Please select your subscription.";
    private static final String NEED_SPECIFY_CLUSTER = "Please select a target cluster.";
    private static final String NEED_SPECIFY_APP_NAME = "Please select a target app.";
    private static final String SERVICE_IS_NOT_READY = "Spring Apps is not ready for deploy, current status is ";
    private static final String TARGET_CLUSTER_DOES_NOT_EXISTS = "Target cluster does not exists.";
    private static final String TARGET_CLUSTER_IS_NOT_AVAILABLE = "Target cluster cannot be found in current subscription";

    @Getter
    @Setter
    @Nullable
    private SpringCloudDeploymentDraft deployment;

    public SpringCloudDeploymentConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.getProject());
//        this.deployment = Optional.ofNullable(element.getChild("SpringCloudAppConfig"))
//            .map(e -> XmlSerializer.deserialize(e, SpringCloudAppConfig.class))
//            .orElseGet(() -> SpringCloudAppConfig.builder().deployment(SpringCloudDeploymentConfig.builder().build()).build());
//        Optional.ofNullable(element.getChild("Artifact"))
//            .map(e -> e.getAttributeValue("identifier"))
//            .map(manager::getAzureArtifactById)
//            .map(a -> new WrappedAzureArtifact(a, this.getProject()))
//            .ifPresent(a -> this.deployment.setArtifact(a));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
//        final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.getProject());
//        final Element appConfigElement = XmlSerializer.serialize(this.deployment, (accessor, o) -> !"artifact".equalsIgnoreCase(accessor.getName()));
//        final IArtifact artifact = this.deployment.getArtifact();
//        Optional.ofNullable(this.deployment)
//            .map(deployment -> XmlSerializer.serialize(deployment, (accessor, o) -> !"artifact".equalsIgnoreCase(accessor.getName())))
//            .ifPresent(element::addContent);
//        Optional.ofNullable(this.deployment)
//            .map(deployment -> (WrappedAzureArtifact) deployment.getArtifact())
//            .map((a) -> a.getArtifact().getIdentifier())
//            .map(id -> new Element("Artifact").setAttribute("identifier", id))
//            .ifPresent(element::addContent);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new Editor(this, getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new SpringCloudDeploymentConfigurationState(getProject(), this);
    }

    @Override
    public void checkConfiguration() {
    }

    static class Factory extends ConfigurationFactory {
        public static final String FACTORY_NAME = "Deploy Spring app";

        public Factory(@NotNull ConfigurationType type) {
            super(type);
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new SpringCloudDeploymentConfiguration(project, this, project.getName());
        }

        @Override
        public RunConfiguration createConfiguration(String name, RunConfiguration template) {
            return new SpringCloudDeploymentConfiguration(template.getProject(), this, name);
        }

        @Override
        public @NotNull String getId() {
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
        protected void resetEditorFrom(@NotNull SpringCloudDeploymentConfiguration config) {
            this.panel.setConfiguration(config);
            if (Objects.nonNull(config.deployment)) {
                AzureTaskManager.getInstance().runLater(() -> this.panel.setValue(config.deployment), AzureTask.Modality.ANY);
            }
        }

        @Override
        protected void applyEditorTo(@NotNull SpringCloudDeploymentConfiguration config) throws ConfigurationException {
            final List<AzureValidationInfo> infos = this.panel.getAllValidationInfos(true);
            final AzureValidationInfo error = infos.stream()
                .filter(i -> !i.isValid())
                .findAny().orElse(null);
            if (Objects.nonNull(error)) {
                final String message = error.getType() == Type.PENDING ? "Please try later after validation" : error.getMessage();
                throw new ConfigurationException(message);
            }
            config.deployment = this.panel.getValue();
        }

        @Override
        protected @NotNull JComponent createEditor() {
            return this.panel.getContentPanel();
        }

        @Override
        public void checkEditorData(SpringCloudDeploymentConfiguration s) {
        }
    }
}
