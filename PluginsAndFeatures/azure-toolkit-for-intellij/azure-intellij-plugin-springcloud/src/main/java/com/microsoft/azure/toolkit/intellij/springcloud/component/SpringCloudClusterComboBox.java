/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterModule;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudClusterComboBox extends AzureComboBox<SpringCloudCluster> {

    private Subscription subscription;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        return ((SpringCloudCluster) item).name();
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

    @Nullable
    @Override
    protected SpringCloudCluster doGetDefaultValue() {
        return CacheManager.getUsageHistory(SpringCloudCluster.class)
            .peek(v -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), v.getSubscriptionId()));
    }

    @NotNull
    @Override
    @AzureOperation(name = "internal/springcloud.list_clusters.subscription", params = {"this.subscription.getId()"})
    protected List<? extends SpringCloudCluster> loadItems() {
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.getId();
            final SpringCloudClusterModule az = Azure.az(AzureSpringCloud.class).clusters(sid);
            return az.list();
        }
        return Collections.emptyList();
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
        final String tooltip = String.format("Create Azure Spring Apps in Azure Portal (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip, () -> {
            final IAccount account = Azure.az(IAzureAccount.class).account();
            final String url = String.format("%s/#create/Microsoft.AppPlatform", account.getPortalUrl());
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url, null);
        });
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }
}
