/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

public class OpenAIPlayground extends BaseEditor {

    private final Project project;
    private final CognitiveAccount account;
    private JPanel pnlRoot;
    private JPanel pnlTitleBar;
    private JPanel pnlBody;
    private JPanel pnlConfigurationContainer;
    private JPanel pnlSystem;
    private JPanel pnlChat;
    private JTabbedPane pnlAssistantTabPane;
    private SystemMessagePanel pnlSystemMessage;
    private JButton btnExport;
    private JButton btnImport;
    private JPanel pnlSystemMessageContainer;
    private ConfigurationPanel pnlConfiguration;
    private ChatBox chatBox1;
    private JPanel pnlExample;

    private CognitiveDeployment deployment;
    private SystemMessage message;

    public OpenAIPlayground(@Nonnull final Project project, @Nonnull final CognitiveAccount account, @Nonnull final VirtualFile virtualFile) {
        this(project, account, null, virtualFile);
    }

    public OpenAIPlayground(@Nonnull final Project project, @Nonnull final CognitiveDeployment deployment, @Nonnull final VirtualFile virtualFile) {
        this(project, deployment.getParent(), deployment, virtualFile);
    }

    public OpenAIPlayground(@Nonnull final Project project, @Nonnull final CognitiveAccount account,
                            @Nullable final CognitiveDeployment deployment, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile);
        this.project = project;
        this.account = account;
        this.deployment = deployment;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.pnlConfiguration.addValueChangedListener(null);
        this.pnlSystemMessage.addValueChangedListener(null);
    }

    @Override
    public @Nonnull JComponent getComponent() {
        return pnlRoot;
    }

    @Override
    public String getName() {
        return String.format("OpenAI Playground (%s)", account.getName());
    }

    @Override
    public void dispose() {

    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
