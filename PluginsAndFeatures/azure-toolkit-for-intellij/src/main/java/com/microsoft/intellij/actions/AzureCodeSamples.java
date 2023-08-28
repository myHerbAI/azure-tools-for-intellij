/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jdesktop.swingx.JXHyperlink;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * Created by vlashch on 6/10/16.
 */
public class AzureCodeSamples extends AnAction {
    @Override
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        JXHyperlink portalLing = new JXHyperlink();
        portalLing.setURI(URI.create("https://azure.microsoft.com/en-us/documentation/samples/?platform=java"));
        portalLing.doClick();
    }
}
