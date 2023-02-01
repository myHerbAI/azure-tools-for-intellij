/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Repository;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ACRImageForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig> {
    @Getter
    private JPanel contentPanel;

    private ACRRegistryComboBox selectorRegistry;
    private ACRRepositoryComboBox selectorRepository;
    private ACRRepositoryTagComboBox selectorTag;

    public ACRImageForm() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        final ContainerRegistry registry = Objects.requireNonNull(this.selectorRegistry.getValue(), "'registry' is required.");
        final Repository repository = Objects.requireNonNull(this.selectorRepository.getValue(), "'repository' is required.");
        final Pair<String, OffsetDateTime> tagWithDate = Objects.requireNonNull(this.selectorTag.getValue(), "'tag' is required.");
        final ContainerAppDraft.ImageConfig config = new ContainerAppDraft.ImageConfig();
        final String fullImageName = String.format("%s/%s:%s", registry.getLoginServerUrl(), repository.getName(), tagWithDate.getLeft());
        config.setContainerRegistry(registry);
        config.setFullImageName(fullImageName);
        return config;
    }

    @Override
    public void setValue(final ContainerAppDraft.ImageConfig config) {
        final ContainerRegistry registry = Objects.requireNonNull(config.getContainerRegistry(), "container registry is null.");
        this.selectorRegistry.setValue(registry);
        this.selectorRepository.setValue(r -> r.getName().equalsIgnoreCase(config.getSimpleImageName()));
        this.selectorTag.setValue(t -> t.getLeft().equalsIgnoreCase(config.getTag()));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.selectorRegistry,
            this.selectorRepository,
            this.selectorTag
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    private void init() {
        this.selectorRegistry.addItemListener(this::onRegistryChanged);
        this.selectorRepository.addItemListener(this::onRepositoryChanged);
        this.selectorRegistry.setRequired(true);
        this.selectorRepository.setRequired(true);
        this.selectorTag.setRequired(true);
    }

    private void onRegistryChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            this.selectorRepository.setRegistry((ContainerRegistry) e.getItem());
        }
    }

    private void onRepositoryChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            this.selectorTag.setRepository(((Repository) e.getItem()));
        }
    }
}
