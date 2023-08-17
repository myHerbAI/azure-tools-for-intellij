package com.microsoft.azure.toolkit.intellij.cognitiveservices.input;

import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.CognitiveAccountComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.CognitiveDeploymentComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Optional;

public class CognitiveDeploymentInputPanel implements AzureFormJPanel<CognitiveDeployment> {
    private JLabel lblSubscription;
    private CognitiveDeploymentComboBox cbDeployment;
    private JPanel pnlRoot;
    private CognitiveAccountComboBox cbAccount;

    public CognitiveDeploymentInputPanel() {
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.cbAccount.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof CognitiveAccount) {
                this.cbDeployment.setAccount((CognitiveAccount) e.getItem());
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbAccount = new CognitiveAccountComboBox();
        this.cbAccount.setUsePreferredSizeAsMinimum(false);
        this.cbDeployment = new CognitiveDeploymentComboBox();
        this.cbDeployment.setUsePreferredSizeAsMinimum(false);
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(CognitiveDeployment data) {
        Optional.ofNullable(data).ifPresent(deployment -> {
            this.cbAccount.setSubscription(data.getSubscription());
            this.cbAccount.setValue(deployment.getParent(), false);
            this.cbDeployment.setValue(data, false);
        });
    }

    @Override
    public CognitiveDeployment getValue() {
        return this.cbDeployment.getValue();
    }
}
