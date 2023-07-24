/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component.resourcegroup;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import org.apache.commons.lang3.StringUtils;

import javax.accessibility.AccessibleRelation;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class FullResourceGroupCreationDialog extends AzureDialog<ResourceGroupDraft>
    implements AzureForm<ResourceGroupDraft> {
    private JPanel contentPanel;
    private JBLabel labelDescription;
    private JLabel lblSubscription;
    private JLabel lblRegion;
    private JLabel lblName;
    private SubscriptionComboBox selectorSubscription;
    private ResourceGroupNameTextField textName;
    private RegionComboBox selectorRegion;

    public FullResourceGroupCreationDialog(@Nullable Subscription subscription) {
        super();
        this.init(subscription);
    }

    protected void init(@Nullable Subscription subscription) {
        super.init();
        if (Objects.nonNull(subscription)) {
            this.selectorSubscription.setValue(subscription);
            this.selectorRegion.setSubscription(subscription);
            this.textName.setSubscription(subscription);
        }
        this.selectorSubscription.putClientProperty(AccessibleRelation.LABELED_BY, this.lblSubscription);
        this.selectorRegion.putClientProperty(AccessibleRelation.LABELED_BY, this.lblRegion);
        this.textName.putClientProperty(AccessibleRelation.LABELED_BY, this.lblName);
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, AzureMessageBundle.message("common.resourceGroup.description").toString());
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
    }

    private void onSubscriptionChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = this.selectorSubscription.getValue();
            this.textName.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
        }
    }

    @Override
    public AzureForm<ResourceGroupDraft> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return AzureMessageBundle.message("common.resourceGroup.create.title").toString();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Nullable
    @Override
    public ResourceGroupDraft getValue() {
        final Subscription subscription = this.selectorSubscription.getValue();
        final Region region = this.selectorRegion.getValue();
        final String name = this.textName.getValue();
        if (Objects.isNull(subscription) || Objects.isNull(region) || StringUtils.isBlank(name)) {
            final String msg = "\"subscription\", \"region\" and \"name\" are all required to create a resource group";
            this.setErrorText(msg);
            AzureMessager.getMessager().warning(msg);
            return null;
        }
        final ResourceGroupDraft draft = Azure.az(AzureResources.class).groups(subscription.getId()).create(name, name);
        draft.setRegion(region);
        return draft;
    }

    @Override
    public void setValue(final ResourceGroupDraft data) {
        this.selectorSubscription.setValue(data.getSubscription());
        this.selectorRegion.setSubscription(data.getSubscription());
        this.selectorRegion.setValue(data.getRegion());
        this.textName.setSubscription(data.getSubscription());
        this.textName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(this.textName, this.selectorSubscription, this.selectorRegion);
    }
}
