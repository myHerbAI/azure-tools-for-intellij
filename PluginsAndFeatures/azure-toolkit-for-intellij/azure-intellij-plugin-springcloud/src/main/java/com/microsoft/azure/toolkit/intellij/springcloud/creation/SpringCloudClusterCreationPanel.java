package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.azure.core.management.Region;
import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.ServiceNameInput;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudRegionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudSkuComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterModule;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SpringCloudClusterCreationPanel extends JPanel implements AzureFormPanel<SpringCloudClusterDraft> {
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private ResourceGroupComboBox selectorResourceGroup;
    private ServiceNameInput textName;
    private SpringCloudRegionComboBox selectorRegion;
    private SpringCloudSkuComboBox selectorSku;
    private JLabel labelPlan;
    private JBLabel descPlan;
    private JCheckBox checkboxTerms;
    private JBLabel textTerms;
    private JLabel labelTerms;

    SpringCloudClusterCreationPanel() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        init();
        initListeners();
    }

    private void init() {
        this.selectorSubscription.setRequired(true);
        this.selectorResourceGroup.setRequired(true);
        this.textName.setRequired(true);
        this.textName.setLabel("Name");
        this.selectorSku.setRequired(true);
        this.selectorRegion.setRequired(true);
        this.labelPlan.setIcon(AllIcons.General.ContextHelp);
        this.labelPlan.setLabelFor(this.selectorSku);
    }

    private void initListeners() {
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorRegion.addItemListener(this::onRegionChanged);
        this.selectorSku.addItemListener(this::onSkuChanged);
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.selectorResourceGroup.setSubscription(subscription);
            this.textName.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
            this.selectorSku.setSubscription(subscription);
        }
    }

    private void onRegionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Region region = (Region) e.getItem();
            this.textName.setRegion(region);
            this.selectorSku.setRegion(region);
        }
    }

    private void onSkuChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Sku sku = (Sku) e.getItem();
            this.selectorRegion.setSku(sku);
            if (Objects.nonNull(sku)) {
                this.textTerms.setVisible(sku.isEnterpriseTier());
                this.labelTerms.setVisible(sku.isEnterpriseTier());
                this.checkboxTerms.setVisible(sku.isEnterpriseTier());
                this.descPlan.setText(StringUtils.SPACE + sku.getDescription());
            }
        }
    }

    @Override
    public void setValue(final SpringCloudClusterDraft data) {
        this.selectorSubscription.setValue(data.getSubscription());
        this.selectorResourceGroup.setValue(data.getResourceGroup());
        this.textName.setValue(data.getName());
        this.selectorRegion.setValue(data.getRegion());
        this.selectorSku.setValue(new com.microsoft.azure.toolkit.lib.springcloud.model.Sku(data.getSku()));
    }

    @Override
    public SpringCloudClusterDraft getValue() {
        final Subscription subscription = Objects.requireNonNull(this.selectorSubscription.getValue());
        final ResourceGroup resourceGroup = Objects.requireNonNull(this.selectorResourceGroup.getValue());
        final Region region = Objects.requireNonNull(this.selectorRegion.getValue());
        if (resourceGroup.isDraftForCreating()) {
            ((ResourceGroupDraft) resourceGroup).setRegion(com.microsoft.azure.toolkit.lib.common.model.Region.fromName(region.name()));
        }
        final Sku sku = Objects.requireNonNull(this.selectorSku.getValue());
        final String serviceName = this.textName.getValue();

        final SpringCloudClusterDraft.Config config = new SpringCloudClusterDraft.Config();
        config.setRegion(region);
        config.setSku(sku.toSku());
        config.setResourceGroup(resourceGroup);

        final SpringCloudClusterModule clusters = Azure.az(AzureSpringCloud.class).clusters(subscription.getId());
        return ((SpringCloudClusterDraft) clusters.create(serviceName, resourceGroup.getName())).withConfig(config);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(this.textName, this.selectorSubscription, this.selectorResourceGroup, this.selectorRegion, this.selectorSku);
    }

    @Override
    public List<AzureValidationInfo> validateAdditionalInfo() {
        if (this.checkboxTerms.isVisible() && !this.checkboxTerms.isSelected()) {
            return Collections.singletonList(AzureValidationInfo.builder().input(this.checkboxTerms).message("Please accept the terms of service").type(AzureValidationInfo.Type.ERROR).build());
        }
        return Collections.emptyList();
    }

    private void createUIComponents() {
        this.descPlan = new JBLabel();
        this.descPlan.setForeground(UIUtil.getLabelInfoForeground());
        this.descPlan.setAllowAutoWrapping(true);
        this.descPlan.setCopyable(true);// this makes label auto wrapping
        this.textTerms = new JBLabel();
        this.textTerms.setAllowAutoWrapping(true);
        this.textTerms.setCopyable(true);
    }
}
