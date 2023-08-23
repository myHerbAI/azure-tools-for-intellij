/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azureexplorer.actions;

import org.eclipse.jface.wizard.WizardDialog;

import com.microsoft.azuretools.azureexplorer.forms.createvm.CreateVMWizard;
import com.microsoft.azuretools.core.components.AzureWizardDialog;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

@Name("Create VM")
public class CreateArmVMAction extends NodeActionListener {
    public CreateArmVMAction(VMArmModule node) {
        super();
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        SignInCommandHandler.requireSignedIn(PluginUtil.getParentShell(), (a) -> {
            CreateVMWizard createVMWizard = new CreateVMWizard((VMArmModule) e.getAction().getNode());
            WizardDialog dialog = new AzureWizardDialog(PluginUtil.getParentShell(), createVMWizard);
            dialog.setTitle("Create new Virtual Machine");
            dialog.create();
            dialog.open();
        });
    }
}
