/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.ModelComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeploymentDraft;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentSku;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CognitiveDeploymentCreationDialog extends AzureDialog<CognitiveDeploymentDraft> implements AzureForm<CognitiveDeploymentDraft> {
    private final CognitiveAccount account;
    private AzureTextInput txtDeploymentName;
    private ModelComboBox cbModel;
    private JPanel pnlRoot;

    public CognitiveDeploymentCreationDialog(@Nonnull final CognitiveAccount account, @Nullable Project project) {
        super(project);
        this.account = account;
        $$$setupUI$$$();
        this.init();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbModel = new ModelComboBox(account);
        this.cbModel.setRequired(true);
    }

    @Override
    public AzureForm<CognitiveDeploymentDraft> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure OpenAI Deployment";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public CognitiveDeploymentDraft getValue() {
        final String deploymentName = txtDeploymentName.getValue();
        final AccountModel model = cbModel.getValue();
        final CognitiveDeploymentDraft draft = account.deployments().create(deploymentName, account.getResourceGroupName());
        final CognitiveDeploymentDraft.Config config = new CognitiveDeploymentDraft.Config();
        // currently, we will use the first sku in model by default
        config.setModel(DeploymentModel.fromAccountModel(Objects.requireNonNull(model)));
        config.setSku(DeploymentSku.fromModelSku(model.getSkus().get(0)));
        draft.setConfig(config);
        return draft;
    }

    @Override
    public void setValue(CognitiveDeploymentDraft data) {
        txtDeploymentName.setValue(data.getName());
        Optional.ofNullable(data.getModel()).ifPresent(model -> cbModel.setValue(m ->
                StringUtils.equalsIgnoreCase(m.getName(), model.getName()) && StringUtils.equalsIgnoreCase(m.getVersion(), model.getVersion())));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtDeploymentName, cbModel);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}

