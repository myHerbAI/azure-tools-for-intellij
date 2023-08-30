package com.microsoft.azure.toolkit.intellij.cognitiveservices.input;

import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.CognitiveAccountComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.GPTDeploymentComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CognitiveDeploymentInputPanel implements AzureFormJPanel<CognitiveDeployment> {
    private GPTDeploymentComboBox cbDeployment;
    private JPanel pnlRoot;
    private CognitiveAccountComboBox cbAccount;
    private JLabel lblDeployment;
    private JLabel lblAccount;

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
        this.cbAccount.setRequired(true);
        this.lblAccount.setLabelFor(this.cbAccount);
        this.cbDeployment = new GPTDeploymentComboBox();
        this.cbDeployment.setUsePreferredSizeAsMinimum(false);
        this.cbDeployment.setRequired(true);
        this.lblDeployment.setLabelFor(this.cbDeployment);
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
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(this.cbAccount, this.cbDeployment);
    }

    @Override
    public CognitiveDeployment getValue() {
        return this.cbDeployment.getValue();
    }
}
