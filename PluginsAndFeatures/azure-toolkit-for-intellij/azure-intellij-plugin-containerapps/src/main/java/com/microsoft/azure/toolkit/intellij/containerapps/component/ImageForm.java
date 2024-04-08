/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ProjectUtils;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Queue;
import java.util.*;

public class ImageForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig> {
    private JPanel pnlRoot;
    private ImageSourceTypeComboBox selectorRegistryType;
    private JPanel formImageContainer;
    private JLabel lblRegistryType;
    private AzureFormJPanel<ContainerAppDraft.ImageConfig> formImage;
    private final List<AzureValueChangeListener<ContainerAppDraft.ImageConfig>> listeners = new ArrayList<>();

    public ImageForm() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.selectorRegistryType.addItemListener(this::onRegistryTypeChanged);
        final String defaultType = ImageSourceTypeComboBox.ACR;
        this.selectorRegistryType.setSelectedItem(defaultType);
        this.updateImagePanel(defaultType);
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(ContainerAppDraft.ImageConfig imageConfig) {
        final ContainerRegistry registry = imageConfig.getContainerRegistry();
        final File source = imageConfig.getSource();
        final String type;
        if (registry != null) {
            type = ImageSourceTypeComboBox.ACR;
        } else if (imageConfig.getFullImageName().startsWith("docker.io")) {
            type = ImageSourceTypeComboBox.DOCKER_HUB;
        } else if (Objects.nonNull(source)) {
            if (Files.exists(source.toPath()) && !source.isDirectory()) {
                type = ImageSourceTypeComboBox.ARTIFACT;
            } else {
                type = ImageSourceTypeComboBox.CODE;
            }
        } else {
            type = ImageSourceTypeComboBox.OTHER;
        }
        this.formImage = this.updateImagePanel(type);
        this.selectorRegistryType.setSelectedItem(type);
        this.formImage.setValue(imageConfig);
    }

    private void onRegistryTypeChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            this.formImage = this.updateImagePanel((String) e.getItem());
        }
    }

    private synchronized AzureFormJPanel<ContainerAppDraft.ImageConfig> updateImagePanel(String type) {
        final GridConstraints constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);
        constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        constraints.setUseParentLayout(true);
        final Project project = ProjectUtils.getProject(this.getContentPanel());
        final AzureFormJPanel<ContainerAppDraft.ImageConfig> newFormImage = switch (type) {
            case ImageSourceTypeComboBox.ACR -> new ACRImageSourceForm();
            case ImageSourceTypeComboBox.CODE -> new CodeSourceForm(project);
            case ImageSourceTypeComboBox.ARTIFACT -> new ArtifactSourceForm(project);
            case ImageSourceTypeComboBox.DOCKER_HUB -> new DockerHubImageForm();
            case ImageSourceTypeComboBox.OTHER -> new OtherPublicRegistryImageForm();
            default -> throw new IllegalArgumentException("Unsupported registry type: " + type);
        };
        this.formImageContainer.removeAll();
        this.formImageContainer.add(newFormImage.getContentPanel(), constraints);
        this.formImageContainer.revalidate();
        this.formImageContainer.repaint();
        this.formImage = newFormImage;
        this.listeners.forEach(this::addValueChangeListenerToAllComponents);
        return newFormImage;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.formImage);
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        return Optional.ofNullable(formImage).map(AzureFormPanel::getValue).orElse(null);
    }

    public void setEnabled(boolean enabled) {
        lblRegistryType.setEnabled(enabled);
        selectorRegistryType.setEnabled(enabled);
        Optional.ofNullable(formImage).map(AzureFormJPanel::getContentPanel).ifPresent(panel -> setEnableForComponent(panel, enabled));
    }

    private void setEnableForComponent(@Nonnull final Component component, final boolean enable) {
        component.setEnabled(enable);
        if (component instanceof Container) {
            for (final Component c : ((Container) component).getComponents()) {
                setEnableForComponent(c, enable);
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        Optional.ofNullable(getContentPanel()).ifPresent(panel -> panel.setVisible(visible));
    }

    public void addValueChangeListenerToAllComponents(final AzureValueChangeListener<ContainerAppDraft.ImageConfig> listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        final Queue<AzureFormInput<?>> inputs = new ArrayDeque<>();
        inputs.add(formImage);
        while (!inputs.isEmpty()) {
            final AzureFormInput<?> input = inputs.poll();
            input.addValueChangedListener(ignore -> listener.accept(this.getValue()));
            if (input instanceof AzureForm<?>) {
                inputs.addAll(((AzureForm<?>) input).getInputs());
            }
        }
    }
}
