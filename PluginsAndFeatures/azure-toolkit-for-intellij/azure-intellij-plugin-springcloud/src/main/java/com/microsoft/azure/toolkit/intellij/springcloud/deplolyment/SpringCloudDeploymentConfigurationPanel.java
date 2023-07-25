/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudDeploymentConfigurationPanel extends JPanel implements AzureFormPanel<SpringCloudDeploymentDraft> {
    @Nonnull
    private final Project project;
    @Setter
    private SpringCloudDeploymentConfiguration configuration;

    @Getter
    private JPanel contentPanel;
    private AzureArtifactComboBox selectorArtifact;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private SpringCloudAppComboBox selectorApp;
    private JLabel lblSubscription;
    private com.intellij.ui.components.JBLabel validationMsg;
    private final Debouncer validateRuntime = new TailingDebouncer(this::validateRuntime, 500);

    public SpringCloudDeploymentConfigurationPanel(SpringCloudDeploymentConfiguration config, @Nonnull final Project project) {
        super();
        this.project = project;
        this.configuration = config;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.selectorArtifact.setFileFilter(virtualFile -> StringUtils.equalsIgnoreCase("jar", FileNameUtils.getExtension(virtualFile.getPath())));
        this.selectorArtifact.addValidator(() -> {
            final AzureArtifact artifact = this.selectorArtifact.getValue();
            if (Objects.nonNull(artifact) && !"jar".equalsIgnoreCase(artifact.getPackaging())) {
                return AzureValidationInfo.error("Invalid artifact, Azure Spring app only supports 'jar' artifact.", this.selectorArtifact);
            }
            return AzureValidationInfo.success(this.selectorArtifact);
        });
        this.selectorArtifact.addItemListener(this::onArtifactChanged);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorCluster.addItemListener(this::onClusterChanged);
        this.selectorApp.addItemListener(this::onAppChanged);
        this.selectorSubscription.setRequired(true);
        this.selectorCluster.setRequired(true);
        this.selectorApp.setRequired(true);
        this.selectorArtifact.setRequired(true);
        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
        this.validationMsg.setIcon(AllIcons.General.Warning);
        this.validationMsg.setAllowAutoWrapping(true);
        this.validationMsg.setCopyable(true);// this makes label auto wrapping
    }

    private void onArtifactChanged(final ItemEvent e) {
        final DataContext context = DataManager.getInstance().getDataContext(getContentPanel());
        final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context);
        final AzureArtifact artifact = (AzureArtifact) e.getItem();
        if (Objects.nonNull(editor) && Objects.nonNull(artifact)) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.configuration.setArtifact(null);
                BuildArtifactBeforeRunTaskUtils.removeBeforeRunTask(editor, artifact, this.configuration);
                this.validateRuntime.debounce();
            }
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.configuration.setArtifact(artifact);
                BuildArtifactBeforeRunTaskUtils.addBeforeRunTask(editor, artifact, this.configuration);
                this.selectorApp.setJavaVersion(artifact.getBytecodeTargetLevel());
                this.validateRuntime.debounce();
            }
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = this.selectorSubscription.getValue();
            this.selectorCluster.setSubscription(subscription);
        }
    }

    private void onClusterChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudCluster cluster = this.selectorCluster.getValue();
            this.selectorApp.setCluster(cluster);
        }
    }

    private void onAppChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudApp app = this.selectorApp.getValue();
            this.configuration.setApp(app);
            this.validateRuntime.debounce();
        }
    }

    private void validateRuntime() {
        final AzureArtifact artifact = this.selectorArtifact.getValue();
        final SpringCloudApp app = this.selectorApp.getValue();
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        if (app == null || artifact == null || app.getParent().isEnterpriseTier()) {
            manager.runLater(() -> this.validationMsg.setVisible(false), AzureTask.Modality.ANY);
            return;
        }
        manager.runOnPooledThread(() -> {
            if (app.getCachedActiveDeployment() == null) {
                manager.runLater(() -> {
                    final String message = "Validating Java runtime version compatibility...";
                    this.validationMsg.setIconWithAlignment(AnimatedIcon.Default.INSTANCE, SwingConstants.LEFT, SwingConstants.CENTER);
                    this.validationMsg.setText(message);
                    this.validationMsg.setVisible(true);
                }, AzureTask.Modality.ANY);
            }
            final Integer appVersion = Optional.of(app).map(SpringCloudApp::getActiveDeployment)
                .map(SpringCloudDeployment::getRuntimeVersion)
                .map(v -> v.split("_")[1]).map(Integer::parseInt).orElse(null);
            final Integer artifactVersion = Optional.of(artifact).map(AzureArtifact::getBytecodeTargetLevel).orElse(null);
            if (Objects.isNull(appVersion) || Objects.isNull(artifactVersion)) {
                manager.runLater(() -> {
                    final String message = Objects.isNull(appVersion) ?
                        String.format("Failed to get app(%s)'s runtime version.", app.getName()) :
                        String.format("Failed to get artifact(%s)'s bytecode version.", app.getName());
                    this.validationMsg.setIconWithAlignment(AllIcons.General.Warning, SwingConstants.LEFT, SwingConstants.CENTER);
                    this.validationMsg.setText(message);
                    this.validationMsg.setVisible(true);
                }, AzureTask.Modality.ANY);
            } else if (appVersion < artifactVersion) {
                manager.runLater(() -> {
                    final String message = String.format("The bytecode version of selected artifact (%s) is \"%d (Java %s)\", " +
                            "which is not compatible with the Java runtime version \"Java %s\" of selected app (%s). " +
                            "Please consider selecting a different app or artifact.",
                        artifact.getName(), artifactVersion + 44, artifactVersion, appVersion, app.getName());
                    this.validationMsg.setIconWithAlignment(AllIcons.General.Error, SwingConstants.LEFT, SwingConstants.TOP);
                    this.validationMsg.setText(message);
                    this.validationMsg.setVisible(true);
                }, AzureTask.Modality.ANY);
            } else {
                manager.runLater(() -> {
                    final String message = "Java runtime version compatibility validation passes.";
                    this.validationMsg.setIconWithAlignment(AllIcons.General.InspectionsOK, SwingConstants.LEFT, SwingConstants.CENTER);
                    this.validationMsg.setText(message);
                    this.validationMsg.setVisible(true);
                }, AzureTask.Modality.ANY);
            }
        });
    }

    @Override
    public synchronized void setValue(@Nonnull SpringCloudDeploymentDraft deploymentDraft) {
        Optional.of(deploymentDraft)
            .map(SpringCloudDeploymentDraft::getArtifact)
            .map(a -> (WrappedAzureArtifact) a)
            .map(WrappedAzureArtifact::getArtifact)
            .ifPresent(a -> this.selectorArtifact.setArtifact(a));
        this.selectorSubscription.setValue(deploymentDraft.getSubscription());
        this.selectorCluster.setValue(deploymentDraft.getParent().getParent());
        this.selectorApp.setValue(deploymentDraft.getParent());
    }

    @Nullable
    @Override
    public SpringCloudDeploymentDraft getValue() {
        final SpringCloudApp app = Objects.requireNonNull(this.selectorApp.getValue(), "target app is not specified.");
        final SpringCloudDeployment deployment = Optional.ofNullable(app.getActiveDeployment()).orElseGet(() -> app.deployments().create("default", null));
        final SpringCloudDeploymentDraft deploymentDraft = (SpringCloudDeploymentDraft) (deployment.isDraft() ? deployment : deployment.update());
        return this.getValue(deploymentDraft);
    }

    public SpringCloudDeploymentDraft getValue(SpringCloudDeploymentDraft deploymentDraft) {
        final AzureArtifact artifact = this.selectorArtifact.getValue();
        if (Objects.nonNull(artifact)) {
            deploymentDraft.setArtifact(new WrappedAzureArtifact(this.selectorArtifact.getValue(), this.project));
        }
        return deploymentDraft;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.selectorApp,
            this.selectorArtifact,
            this.selectorSubscription,
            this.selectorCluster
        };
        return Arrays.asList(inputs);
    }

    private void createUIComponents() {
        this.selectorArtifact = new AzureArtifactComboBox(project);
        this.selectorArtifact.reloadItems();
    }
}
