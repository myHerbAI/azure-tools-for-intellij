/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.input;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.CognitiveSubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.creation.CognitiveAccountCreationDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.swing.*;
import java.util.Optional;

public class CognitiveSubscriptionInputPanel implements AzureFormJPanel<Subscription> {
    private JLabel lblSubscription;
    private CognitiveSubscriptionComboBox cbSubscription;
    private JBLabel lblRegister;
    private JPanel pnlRoot;

    private AzureEventBus.EventListener eventListener;

    public CognitiveSubscriptionInputPanel() {
        $$$setupUI$$$();
        this.lblRegister.setText(CognitiveAccountCreationDialog.REGISTER_SUBSCRIPTION_TEXT);
        this.lblRegister.setFont(JBFont.regular());
        this.lblRegister.setVisible(false);
        this.lblRegister.setIconWithAlignment(AllIcons.General.Warning, SwingConstants.LEFT, SwingConstants.TOP);
        this.lblRegister.setAllowAutoWrapping(true);
        this.lblRegister.setCopyable(true);// this makes label auto wrapping
        this.lblRegister.setForeground(UIUtil.getContextHelpForeground());

        this.cbSubscription.setUsePreferredSizeAsMinimum(false);
        this.cbSubscription.addItemListener(e -> {
            final Subscription subscription = (Subscription) e.getItem();
            final boolean openAIEnabled = Optional.ofNullable(subscription)
                    .map(s -> Azure.az(AzureCognitiveServices.class).isOpenAIEnabled(s.getId())).orElse(false);
            lblRegister.setVisible(!openAIEnabled);
        });
        this.eventListener = new AzureEventBus.EventListener(ignore -> this.cbSubscription.reloadItems());
        AzureEventBus.on("account.logged_in.account", eventListener);
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(Subscription data) {
        cbSubscription.setValue(data);
    }

    @Override
    public Subscription getValue() {
        return cbSubscription.getValue();
    }
}
