/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.AccessibilityUtils;

public class SignInWindow extends AzureTitleAreaDialogWrapper {
    private static final String DESC = "desc_label";
    private static final String AZURE_SIGN_IN = "Azure Sign In";
    private Button cliBtn;
    private Button oauthBtn;
    private Button deviceBtn;
    private Button spBtn;
    private Label cliDesc;
    private Label oauthDesc;
    private Label deviceDesc;
    private Label spDesc;

    private AuthType type = null;
	private Button okButton;
	private Group authTypeGroup;
	private AuthType data;

    /**
     * Create the dialog.
     * @param parentShell
     *
     */
    public SignInWindow(Shell parentShell) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        this.okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Sign in");
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(AZURE_SIGN_IN);
        setTitle(AZURE_SIGN_IN);
        getShell().setText(AZURE_SIGN_IN);
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new FillLayout(SWT.HORIZONTAL));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        this.authTypeGroup = new Group(composite, SWT.NONE);
        authTypeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        authTypeGroup.setText("Authentication Method");
        authTypeGroup.setLayout(new GridLayout(1, false));
        cliBtn = createRadioButton(authTypeGroup, "Azure CLI (checking...)", AuthType.AZURE_CLI);
        cliDesc = createDescriptionLabel(authTypeGroup, cliBtn, "Consume your existing Azure CLI credential.");

        oauthBtn = createRadioButton(authTypeGroup, "OAuth2", AuthType.OAUTH2);
        oauthDesc = createDescriptionLabel(authTypeGroup, oauthBtn, "You will need to open an external browser and sign in.");

        deviceBtn = createRadioButton(authTypeGroup, "Device Login", AuthType.DEVICE_CODE);
        deviceDesc = createDescriptionLabel(authTypeGroup, deviceBtn, "You will need to open an external browser and sign in with a generated device code.");

        spBtn = createRadioButton(authTypeGroup, "Service Principal", AuthType.SERVICE_PRINCIPAL);
        spDesc = createDescriptionLabel(authTypeGroup, spBtn, "Use Azure Active Directory service principal for sign in.");

        this.updateSelection();
        checkAccountAvailability();
        return area;
    }

    private Button createRadioButton(Composite parent, String label, AuthType type) {
        final Button radioButton = new Button(parent, SWT.RADIO);
        radioButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        radioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                SignInWindow.this.updateSelection();
            }
        });
        radioButton.setText(label);
        return radioButton;
    }

    private Label createDescriptionLabel(Composite parent, Button button, String description) {
        Composite compositeDevice = new Composite(parent, SWT.NONE);
        GridData gdCompositeDevice = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdCompositeDevice.heightHint = 38;
        gdCompositeDevice.widthHint = 66;
        compositeDevice.setLayoutData(gdCompositeDevice);
        compositeDevice.setLayout(new GridLayout(1, false)          );
        Label label = new Label(compositeDevice, SWT.WRAP); 
        GridData gdLblDeviceInfo = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdLblDeviceInfo.horizontalIndent = 11;
        label.setLayoutData(gdLblDeviceInfo);
        label.setText(description);
        AccessibilityUtils.addAccessibilityNameForUIComponent(button, button.getText() + " "+ description);
        return label;
        //
    }

    private void checkAccountAvailability() {
        // only azure cli need availability check.
        this.oauthBtn.setEnabled(true);
        this.deviceBtn.setEnabled(true);
        this.spBtn.setEnabled(true);
        this.cliBtn.setText("Azure CLI (checking...)");
        this.cliBtn.setEnabled(false);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final boolean available = AuthType.AZURE_CLI.checkAvailable();
            AzureTaskManager.getInstance().runLater(()->{
                cliBtn.setEnabled(available);
                cliBtn.setText(available ? "Azure CLI" : "Azure CLI (Not logged in)");
                if (cliBtn.getSelection() && !available) {
                    oauthBtn.setSelection(true);
                }
                updateSelection();
            });
        });
    }

    private void updateSelection() {
        boolean selectionAvailable = false;
        for (final Control control : authTypeGroup.getChildren()) {
        	if(control instanceof Button) {
            	final Button button = (Button) control;
                final Label label = ((Label) button.getData(DESC));
            	if(Objects.nonNull(label)) {
                    label.setEnabled(button.getSelection() && button.isEnabled());
            	}
                selectionAvailable = selectionAvailable || (button.getSelection() && button.isEnabled());
        	}
        }
    	if(Objects.nonNull(this.okButton)) {
            this.okButton.setEnabled(selectionAvailable);
    	}
    }

    @Override
	protected void okPressed() {
		if (this.cliBtn.getSelection()) {
			this.data = AuthType.AZURE_CLI;
		} else if (this.oauthBtn.getSelection()) {
			this.data = AuthType.OAUTH2;
		} else if (this.deviceBtn.getSelection()) {
			this.data = AuthType.DEVICE_CODE;
		} else if (this.spBtn.getSelection()) {
			this.data = AuthType.SERVICE_PRINCIPAL;
		} else {
			throw new AzureToolkitRuntimeException("No auth type is selected");
		}
		super.okPressed();
	}

	public AuthType getData() {
		return this.data;
    }
}
