/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.creation.SpringCloudAppCreationDialog;
import com.microsoft.azure.toolkit.intellij.springcloud.creation.SpringCloudClusterCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class SpringCloudClusterComboBox extends AzureComboBox<SpringCloudCluster> {
    private final List<SpringCloudCluster> draftItems = new LinkedList<>();
    private Subscription subscription;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        final SpringCloudCluster cluster = (SpringCloudCluster) item;
        return cluster.isDraftForCreating() ? String.format("(New) %s", cluster.getName()) : cluster.getName();
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable SpringCloudCluster val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating() && !this.draftItems.contains(val)) {
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected SpringCloudCluster doGetDefaultValue() {
        return CacheManager.getUsageHistory(SpringCloudCluster.class)
            .peek(v -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), v.getSubscriptionId()));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/springcloud.list_clusters.subscription", params = {"this.subscription.getId()"})
    protected List<? extends SpringCloudCluster> loadItems() {
        final List<SpringCloudCluster> clusters = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.getId();
            if (!this.draftItems.isEmpty()) {
                clusters.addAll(this.draftItems.stream().filter(c -> c.getSubscriptionId().equals(sid)).toList());
            }
            final SpringCloudClusterModule az = Azure.az(AzureSpringCloud.class).clusters(sid);
            clusters.addAll(az.list());
        }
        return clusters;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureSpringCloud.class).clusters(s.getId()).refresh());
        super.refreshItems();
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final List<ExtendableTextComponent.Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create Azure Spring Apps (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip, this::showClusterCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void showClusterCreationPopup() {
        final SpringCloudClusterCreationDialog dialog = new SpringCloudClusterCreationDialog(null);
        dialog.setOkActionListener((draft) -> {
            dialog.close();
            this.setValue(draft);
        });
        dialog.show();
    }
}
