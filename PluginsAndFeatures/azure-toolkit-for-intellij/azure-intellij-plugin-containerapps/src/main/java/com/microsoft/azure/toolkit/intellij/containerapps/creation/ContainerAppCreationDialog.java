/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.containerapps.component.*;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppModule;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

@Slf4j
public class ContainerAppCreationDialog extends AzureDialog<ContainerAppDraft.Config> implements AzureForm<ContainerAppDraft.Config> {
    private static final Pattern CONTAINER_APP_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9\\-]{0,30}[a-z0-9]$");
    private static final String CONTAINER_APP_NAME_VALIDATION_MESSAGE = "A name must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. The length must not be more than 32 characters.";
    private JLabel lblSubscription;
    private SubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private JLabel lblContainerAppName;
    private AzureTextInput txtContainerAppName;
    private JLabel lblRegion;
    private RegionComboBox cbRegion;
    private JPanel pnlRoot;
    private AzureContainerAppsEnvironmentComboBox cbEnvironment;
    private AzureHideableTitledSeparator titleIngress;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private AzureHideableTitledSeparator titleAppSettings;
    private AzureHideableTitledSeparator titleProjectDetails;
    private AzureHideableTitledSeparator titleContainerAppsEnvironment;
    private AzureHideableTitledSeparator titleEnv;
    private JLabel lblEnv;
    private JPanel pnlEnv;
    private JPanel pnlIngressSettingsHolder;
    private JPanel pnlAppSettings;
    private JPanel pnlProjectDetails;
    private JPanel pnlContainerAppsEnvironment;
    private IngressConfigurationPanel pnlIngressConfiguration;
    private JRadioButton btnDeployCode;
    private JRadioButton btnDeployArtifact;
    private JRadioButton btnDeployImage;
    private ImageForm pnlImage;
    private CodeForm pnlCode;
    private ArtifactForm pnlArtifact;

    private IImageForm formImage;

    public static final IngressConfig QUICK_START_INGRESS = IngressConfig.builder().enableIngress(true).external(true).targetPort(80).build();

    public ContainerAppCreationDialog(final Project project) {
        super(project);
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.cbRegion.setRequired(true);
        this.txtContainerAppName.setRequired(true);

        this.txtContainerAppName.addValidator(this::validateContainerAppName);
        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbRegion.addItemListener(this::onRegionChanged); // trigger validation after resource group changed
        this.cbResourceGroup.addItemListener(this::onResourceGroupChanged);

        this.btnDeployCode.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployArtifact.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployImage.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployCode.setSelected(true);
        this.pnlCode.setVisible(true);
        this.pnlImage.setVisible(false);
        this.pnlArtifact.setVisible(false);
        this.pnlImage.setOnImageFormChanged(this::onImageFormChanged);
        this.pnlCode.setOnFolderChanged(this::onSelectedFolderChanged);
        this.formImage = this.pnlCode;

        this.lblSubscription.setLabelFor(cbSubscription);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.lblContainerAppName.setLabelFor(txtContainerAppName);
        this.lblRegion.setLabelFor(cbRegion);

        this.titleProjectDetails.addContentComponent(pnlProjectDetails);
        this.titleContainerAppsEnvironment.addContentComponent(pnlContainerAppsEnvironment);
        this.titleAppSettings.addContentComponent(pnlAppSettings);
        this.titleIngress.addContentComponent(pnlIngressSettingsHolder);
        this.titleEnv.addContentComponent(pnlEnv);

        this.titleProjectDetails.expand();
        this.titleContainerAppsEnvironment.expand();
        this.titleAppSettings.expand();
        this.titleIngress.collapse();
        this.titleEnv.expand();
    }

    private void onImageFormChanged(final String type) {
        final boolean useQuickStartImage = this.formImage == this.pnlImage && Objects.equals(type, ImageSourceTypeComboBox.QUICK_START);
        if (useQuickStartImage) {
            pnlIngressConfiguration.setValue(QUICK_START_INGRESS);
        }
        this.titleIngress.toggle(!useQuickStartImage);
        this.titleIngress.setEnabled(!useQuickStartImage);
        this.pnlIngressConfiguration.setEnabled(!useQuickStartImage);
        this.lblEnv.setEnabled(!useQuickStartImage);
        this.inputEnv.setEnabled(!useQuickStartImage);
    }

    private void onSelectedFolderChanged(final Path folder) {
        if (this.formImage != this.pnlCode && Objects.nonNull(folder) && Files.isDirectory(folder)) {
            return;
        }
        final Path dockerfile = folder.resolve("Dockerfile");
        if (Files.isRegularFile(dockerfile)) {
            try {
                final List<Integer> ports = AzureDockerClient.getExposedPortsOfDockerfile(dockerfile.toFile());
                if (!ports.isEmpty()) {
                    final IngressConfig ingressConfig = IngressConfig.builder().enableIngress(true).external(true).targetPort(ports.get(0)).build();
                    this.pnlIngressConfiguration.setValue(ingressConfig);
                }
            } catch (final IOException e) {
                log.error("Failed to parse Dockerfile", e);
            }
        }
    }

    private void mergeContainerConfiguration(final ImageForm target, final ContainerAppDraft.ImageConfig value) {
        try {
            final ContainerAppDraft.ImageConfig targetValue = target.getValue();
            if (ObjectUtils.allNotNull(targetValue, value)) {
                if (!Objects.equals(Objects.requireNonNull(targetValue).getContainerRegistry(), value.getContainerRegistry()) ||
                    !Objects.equals(targetValue.getFullImageName(), value.getFullImageName())) {
                    target.setValue(value);
                }
            }
        } catch (final RuntimeException e) {
            // swallow exception as required parameters may be null
            target.setValue(value);
        }
    }

    private void onResourceGroupChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof ResourceGroup) {
            final ResourceGroup resourceGroup = (ResourceGroup) itemEvent.getItem();
            this.cbEnvironment.setResourceGroup(resourceGroup);
        }
    }

    private void onRegionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Region) {
            final Region region = (Region) itemEvent.getItem();
            this.txtContainerAppName.validateValueAsync();
            this.cbEnvironment.setRegion(region);
        }
    }

    private void onSubscriptionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) itemEvent.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
            this.cbEnvironment.setSubscription(subscription);
        }
    }

    private AzureValidationInfo validateContainerAppName() {
        final String name = txtContainerAppName.getValue();
        final ContainerAppsEnvironment value = cbEnvironment.getValue();
        if (value != null && !value.isDraftForCreating()) {
            final Availability availability = value.checkContainerAppNameAvailability(name);
            return availability.isAvailable() ? AzureValidationInfo.success(txtContainerAppName) :
                AzureValidationInfo.error(availability.getUnavailabilityMessage(), txtContainerAppName);
        } else {
            final Matcher matcher = CONTAINER_APP_NAME_PATTERN.matcher(name);
            return matcher.matches() && !StringUtils.contains(name, "--") ? AzureValidationInfo.success(txtContainerAppName) :
                AzureValidationInfo.error(CONTAINER_APP_NAME_VALIDATION_MESSAGE, txtContainerAppName);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbEnvironment = new AzureContainerAppsEnvironmentComboBox();
        this.cbEnvironment.setRequired(true);
        this.pnlCode = new CodeForm(this.project);
        this.pnlArtifact = new ArtifactForm(this.project);
    }

    private void onDeploymentSourceChanged(ItemEvent event) {
        this.pnlImage.setVisible(this.btnDeployImage.isSelected());
        this.pnlArtifact.setVisible(this.btnDeployArtifact.isSelected());
        this.pnlCode.setVisible(this.btnDeployCode.isSelected());

        this.formImage = this.btnDeployImage.isSelected() ? this.pnlImage :
            this.btnDeployArtifact.isSelected() ? this.pnlArtifact : this.pnlCode;
        this.onImageFormChanged(this.pnlImage.getRegistryType());
        this.onSelectedFolderChanged(this.pnlCode.getSourceFolder());
    }

    @Override
    public AzureForm<ContainerAppDraft.Config> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Container App";
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public ContainerAppDraft.Config getValue() {
        final ContainerAppDraft.Config result = new ContainerAppDraft.Config();
        result.setSubscription(cbSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setName(txtContainerAppName.getValue());
        result.setRegion(cbRegion.getValue());
        result.setEnvironment(cbEnvironment.getValue());
        result.setIngressConfig(pnlIngressConfiguration.getValue());

        // set app for image form so that it can get correct ImageConfig
        final ContainerAppModule module = az(AzureContainerApps.class).containerApps(result.getSubscription().getId());
        final ContainerAppDraft draft = module.create(result.getName(), result.getResourceGroup().getName());
        draft.setConfig(result);
        this.formImage.setContainerApp(draft);

        final ContainerAppDraft.ImageConfig imageConfig = this.formImage.getValue();
        final List<EnvironmentVar> vars = inputEnv.getEnvironmentVariables().entrySet().stream()
            .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
            .collect(Collectors.toList());
        Optional.ofNullable(imageConfig).ifPresent(config -> config.setEnvironmentVariables(vars));
        result.setImageConfig(imageConfig);
        return result;
    }

    @Override
    public void setValue(ContainerAppDraft.Config data) {
        Optional.ofNullable(data.getSubscription()).ifPresent(cbSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getName()).ifPresent(txtContainerAppName::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(cbRegion::setValue);
        Optional.ofNullable(data.getEnvironment()).ifPresent(cbEnvironment::setValue);
        final ContainerAppDraft.ImageConfig imageConfig = data.getImageConfig();
        if (Objects.nonNull(imageConfig)) {
            final Optional<Path> source = Optional.ofNullable(imageConfig.getBuildImageConfig()).map(ContainerAppDraft.BuildImageConfig::getSource);
            if (source.isEmpty()) {
                this.btnDeployImage.setSelected(true);
                this.pnlImage.setValue(imageConfig);
            } else if (source.map(Files::isDirectory).orElse(false)) {
                this.btnDeployCode.setSelected(true);
                this.pnlCode.setValue(imageConfig);
            } else {
                this.btnDeployArtifact.setSelected(true);
                this.pnlArtifact.setValue(imageConfig);
            }
            this.inputEnv.setEnvironmentVariables(imageConfig.getEnvironmentVariables().stream()
                .collect(Collectors.toMap(EnvironmentVar::name, EnvironmentVar::value)));
            Optional.ofNullable(data.getIngressConfig()).ifPresent(pnlIngressConfiguration::setValue);
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbResourceGroup, txtContainerAppName, cbRegion, this.formImage);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
