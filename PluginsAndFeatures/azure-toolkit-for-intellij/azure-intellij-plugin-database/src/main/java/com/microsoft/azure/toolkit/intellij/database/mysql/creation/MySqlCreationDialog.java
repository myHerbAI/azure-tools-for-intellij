/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql.creation;

import com.google.common.base.Preconditions;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class MySqlCreationDialog extends AzureDialog<DatabaseServerConfig> {
    private static final String DIALOG_TITLE = "Create Azure Database for MySQL";
    private JPanel rootPanel;
    private MySqlCreationBasicPanel basic;
    private MySqlCreationAdvancedPanel advanced;

    private boolean advancedMode;
    private JCheckBox checkboxMode;

    public MySqlCreationDialog(@Nullable Project project) {
        super(project);
        init();
    }

    @Override
    protected void init() {
        super.init();
        advanced.setVisible(false);
    }

    @Override
    public AzureForm<DatabaseServerConfig> getForm() {
        return this.advancedMode ? advanced : basic;
    }

    @Override
    protected String getDialogTitle() {
        return DIALOG_TITLE;
    }

    @Override
    protected JComponent createDoNotAskCheckbox() {
        this.checkboxMode = new JCheckBox(AzureMessageBundle.message("common.moreSetting").toString());
        this.checkboxMode.setVisible(true);
        this.checkboxMode.setSelected(false);
        this.checkboxMode.addActionListener(e -> this.toggleAdvancedMode(this.checkboxMode.isSelected()));
        return this.checkboxMode;
    }

    protected void toggleAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
        if (advancedMode) {
            advanced.setValue(basic.getValue());
        } else {
            basic.setValue(advanced.getValue());
        }
        advanced.setVisible(advancedMode);
        basic.setVisible(!advancedMode);
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return advancedMode ? advanced.getServerNameTextField() : basic.getServerNameTextField();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }

    private void createUIComponents() {
        final DatabaseServerConfig config = getDefaultConfig();
        basic = new MySqlCreationBasicPanel(config);
        advanced = new MySqlCreationAdvancedPanel(config);
    }

    public static DatabaseServerConfig getDefaultConfig() {
        final String defaultNameSuffix = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        final List<Subscription> subs = az(AzureAccount.class).account().getSelectedSubscriptions();
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(subs), "There are no subscriptions in your account.");

        final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
        final Subscription subscription = Optional.ofNullable(historySub).orElse(subs.get(0));
        final List<Region> regions = az(AzureAccount.class).listRegions(subscription.getId());
        final Region historyRegion = CacheManager.getUsageHistory(Region.class).peek(regions::contains);
        final Region region = Optional.ofNullable(historyRegion).orElse(Region.US_EAST);
        final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class).peek(r -> r.getSubscriptionId().equals(subscription.getId()));
        final String rgName = "rs-" + defaultNameSuffix;
        final ResourceGroupDraft rg = az(AzureResources.class).groups(subscription.getId()).create(rgName, rgName);
        rg.setRegion(region);
        final DatabaseServerConfig config = new DatabaseServerConfig("mysql-" + defaultNameSuffix, region);
        config.setSubscription(subscription);
        config.setResourceGroup(Optional.ofNullable(historyRg).orElse(rg));
        config.setAdminName(Optional.ofNullable(System.getProperty("user.name")).map(n -> n.replaceAll("[^A-Za-z0-9 ]", "")).orElse(null));
        config.setAdminPassword(StringUtils.EMPTY);
        config.setVersion("5.7"); // default to 11
        return config;
    }

}
