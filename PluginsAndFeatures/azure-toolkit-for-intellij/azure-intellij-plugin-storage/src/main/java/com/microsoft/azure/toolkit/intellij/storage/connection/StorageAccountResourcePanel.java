/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class StorageAccountResourcePanel implements AzureFormJPanel<Resource<StorageAccount>> {
    public static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select an existing Azure Storage account.</html>";
    protected SubscriptionComboBox subscriptionComboBox;
    protected AzureComboBox<StorageAccount> accountComboBox;
    @Getter
    protected JPanel contentPanel;
    private JPanel pnlAzure;
    private JRadioButton btnAzure;
    private JRadioButton btnLocal;
    private JLabel lblSubScription;
    private JLabel lblEnvironment;
    private JLabel lblAccount;
//    private AzureEventBus.EventListener signInOutListener;

    public StorageAccountResourcePanel() {
        this.init();
    }

    private void init() {
        final ButtonGroup environmentGroup = new ButtonGroup();
        environmentGroup.add(btnAzure);
        environmentGroup.add(btnLocal);
        btnAzure.addItemListener(ignore -> onSelectEnvironment());
        btnLocal.addItemListener(ignore -> onSelectEnvironment());

        btnAzure.setSelected(true);
        this.onSelectEnvironment();

        this.subscriptionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.accountComboBox.reloadItems();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.accountComboBox.clear();
            }
        });

        lblSubScription.setLabelFor(subscriptionComboBox);
        lblEnvironment.setLabelFor(btnAzure);
        lblAccount.setLabelFor(accountComboBox);
    }

    private void onSelectEnvironment() {
        pnlAzure.setVisible(btnAzure.isSelected());
        accountComboBox.setRequired(btnAzure.isSelected());
        if (Objects.nonNull(accountComboBox.getValidationInfo())) {
            accountComboBox.validateValueAsync();
        }
    }

    @Override
    public void setValue(Resource<StorageAccount> accountResource) {
        final StorageAccount account = accountResource.getData();
        Optional.ofNullable(account).ifPresent((a -> {
            if (a instanceof AzuriteStorageAccount) {
                btnLocal.setSelected(true);
            } else {
                btnAzure.setSelected(true);
                this.subscriptionComboBox.setValue(a.getSubscription());
                this.accountComboBox.setValue(a);
            }
        }));
    }

    @Nullable
    @Override
    public Resource<StorageAccount> getValue() {
        final AzureValidationInfo info = this.getValidationInfo(true);
        if (!info.isValid()) {
            return null;
        }
        final StorageAccount account = btnAzure.isSelected() ? this.accountComboBox.getValue() : AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT;
        return StorageAccountResourceDefinition.INSTANCE.define(account);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
            this.accountComboBox,
            this.subscriptionComboBox
        );
    }

    protected void createUIComponents() {
        final Supplier<List<? extends StorageAccount>> loader = () -> Optional
                .ofNullable(this.subscriptionComboBox)
                .map(AzureComboBox::getValue)
                .map(Subscription::getId)
                .map(id -> Azure.az(AzureStorageAccount.class).accounts(id).list())
                .orElse(Collections.emptyList());
        this.accountComboBox = new AzureComboBox<>(loader) {

            @Nullable
            @Override
            protected StorageAccount doGetDefaultValue() {
                return CacheManager.getUsageHistory(StorageAccount.class)
                    .peek(v -> Objects.isNull(subscriptionComboBox) || Objects.equals(subscriptionComboBox.getValue(), v.getSubscription()));
            }

            @Override
            protected String getItemText(Object item) {
                return Optional.ofNullable(item).map(i -> ((StorageAccount) i).getName()).orElse(StringUtils.EMPTY);
            }

            @Override
            protected void refreshItems() {
                Optional.ofNullable(StorageAccountResourcePanel.this.subscriptionComboBox)
                    .map(AzureComboBox::getValue)
                    .map(Subscription::getId)
                    .ifPresent(id -> Azure.az(AzureStorageAccount.class).accounts(id).refresh());
                super.refreshItems();
            }
        };
    }
}
