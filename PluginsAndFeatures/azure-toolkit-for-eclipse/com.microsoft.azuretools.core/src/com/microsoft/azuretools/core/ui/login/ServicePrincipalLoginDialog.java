/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azuretools.core.ui.login;

import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureDialog;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureTextInput;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ServicePrincipalLoginDialog extends AzureDialog<AuthConfiguration> {
    private ServicePrincipalLoginPanel loginPanel;

    public ServicePrincipalLoginDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        loginPanel = new ServicePrincipalLoginPanel(container, SWT.NONE);
        loginPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        return container;
    }

    @Override
    protected String getDialogTitle() {
        return "Sign In - Service Principal";
    }

    @Override
    public AzureForm<AuthConfiguration> getForm() {
        return this.loginPanel;
    }

	public AuthConfiguration getValue() {
		return this.loginPanel.getValue();
	}

    @Override
    protected Point getInitialSize() {
        this.getShell().layout();
        return this.getShell().computeSize(450, 440, true);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
        newShell.setMinimumSize(300, 340);
    }

    private class ServicePrincipalLoginPanel extends Composite implements AzureForm<AuthConfiguration> {
        private final Button btnOpenFileButton;
        private final AtomicBoolean intermediateState = new AtomicBoolean(false);
        private final AzureTextInput txtTenantId;
        private final AzureTextInput txtClientId;
        private final AzureTextInput txtPassword;
        private final AzureTextInput txtCertificate;
        private final Text txtJson;
        private final Button radioPassword;
        private final Button radioCertificate;

        /**
         * Create the composite.
         *
         * @param parent
         * @param style
         */
        public ServicePrincipalLoginPanel(Composite parent, int style) {
            super(parent, style);

            Composite container = this;
            setLayout(new GridLayout(2, false));

            Label lblTenantId = new Label(container, SWT.NONE);
            lblTenantId.setText("Tenant Id: ");

            txtTenantId = new GuidAzureTextInput(container);
            txtTenantId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            txtTenantId.setLabeledBy(lblTenantId);
            txtTenantId.setRequired(true);


            Label lblClientId = new Label(container, SWT.NONE);
            lblClientId.setText("Client Id: ");


            txtClientId = new GuidAzureTextInput(container);
            txtClientId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            txtClientId.setRequired(true);
            txtClientId.setLabeledBy(lblClientId);

            Label lblSecret = new Label(container, SWT.NONE);
            lblSecret.setText("Secret: ");

            Group composite = new Group(container, SWT.NONE);
            composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            GridLayout glComposite = new GridLayout(2, false);
            glComposite.verticalSpacing = 0;
            glComposite.marginHeight = 0;
            glComposite.marginTop = 1;
            glComposite.marginRight = 5;
            glComposite.marginLeft = 5;
            glComposite.marginWidth = 0;
            composite.setLayout(glComposite);

            radioPassword = new Button(composite, SWT.RADIO);
            radioPassword.setSelection(true);
            radioPassword.setText("Password:");

            txtPassword = new AzureTextInput(composite, SWT.BORDER | SWT.PASSWORD);
            txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            txtPassword.setLabel("Password");
            radioCertificate = new Button(composite, SWT.RADIO);
            radioCertificate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
            radioCertificate.setText("Certificate:");

            Composite compositeCert = new Composite(composite, SWT.NONE);
            compositeCert.setLayout(new FormLayout());
            GridData gdCompositeCert = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
            gdCompositeCert.widthHint = 191;
            compositeCert.setLayoutData(gdCompositeCert);

            txtCertificate = new AzureTextInput(compositeCert, SWT.BORDER | SWT.READ_ONLY);
            txtCertificate.setEnabled(false);
            FormData fdTxtCertificate = new FormData();
            fdTxtCertificate.top = new FormAttachment(0, 3);
            fdTxtCertificate.left = new FormAttachment(0, 0);
            txtCertificate.setLayoutData(fdTxtCertificate);
            txtCertificate.setLabel("Certificate");

            btnOpenFileButton = new Button(compositeCert, SWT.CENTER);
            fdTxtCertificate.right = new FormAttachment(btnOpenFileButton, -6);
            FormData fdBtnOpenFileButton = new FormData();
            fdBtnOpenFileButton.top = new FormAttachment(0, 1);
            fdBtnOpenFileButton.right = new FormAttachment(100);
            btnOpenFileButton.setLayoutData(fdBtnOpenFileButton);
            btnOpenFileButton.setText("...");
            btnOpenFileButton.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {
                }

                public void widgetSelected(SelectionEvent e) {
                    FileDialog dlg = new FileDialog(btnOpenFileButton.getShell(), SWT.OPEN);
                    dlg.setText("Open");
                    String path = dlg.open();
                    if (path == null) {
                        return;
                    }
                    txtCertificate.setText(path);
                }
            });
            btnOpenFileButton.setEnabled(false);

            Label lblJson = new Label(container, SWT.NONE);
            lblJson.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true, 1, 1));
            lblJson.setText("JSON:");

            Composite composite2 = new Composite(container, SWT.NONE);
            composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
            composite2.setLayout(new FillLayout(SWT.HORIZONTAL));

            txtJson = new Text(composite2, SWT.MULTI | SWT.BORDER);
            txtJson.setLayoutData(new GridData(GridData.FILL_BOTH));

            txtJson.addModifyListener(event -> {
                this.json2UIComponents();
            });

            radioCertificate.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                    triggerPasswordCertState();
                }
            });

            radioPassword.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                    triggerPasswordCertState();
                }
            });

            Stream.of(txtCertificate, txtClientId, txtPassword, txtTenantId).forEach(d -> d.addModifyListener(r -> updateToJsonEditor()));
            Stream.of(radioCertificate, radioPassword).forEach(d -> d.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                    updateToJsonEditor();
                }
            }));

        }

        private void triggerPasswordCertState() {
            boolean selection = radioCertificate.getSelection();
            btnOpenFileButton.setEnabled(selection);
            txtCertificate.setEnabled(selection);
            txtPassword.setEnabled(!selection);
            List<AzureValidationInfo> infos = ServicePrincipalLoginDialog.this.doValidateAll();
            // workaround: clear validation information
            getInputs().forEach(ctrl -> ctrl.setValidationInfo(AzureValidationInfo.success(ctrl)));
            setErrorInfoAll(infos);
            setOkButtonEnabled(infos.isEmpty());
        }

        private void updateToJsonEditor() {
            if (!intermediateState.compareAndSet(false, true)) {
                return;
            }
            try {
                AuthConfiguration newData = getValue();
                Map<String, String> map = new LinkedHashMap<>();
                if (newData.getKey() == null) {
                    map.put("fileWithCertAndPrivateKey", newData.getCertificate());
                } else {
                    String password = StringUtils.isNotBlank(newData.getKey()) ? "<hidden>" : "<empty>";
                    map.put("password", password);
                }
                map.put("appId", newData.getClient());
                map.put("tenant", newData.getTenant());
                String text = JsonUtils.toJson(map);
                this.txtJson.setText(text);
            } finally {
                intermediateState.set(false);
            }
        }

        private void json2UIComponents() {
            if (txtJson.isDisposed()) {
                return;
            }

            try {
                if (!intermediateState.compareAndSet(false, true)) {
                    return;
                }
                String json = this.txtJson.getText();
                Map<String, String> map = JsonUtils.fromJson(json, HashMap.class);
                if (map == null) {
                    return;
                }
                AuthConfiguration newData = new AuthConfiguration(AuthType.SERVICE_PRINCIPAL);
                if (map.containsKey("appId")) {
                    newData.setClient(StringUtils.defaultString(map.get("appId")));
                }

                if (map.containsKey("tenant")) {
                    newData.setTenant(StringUtils.defaultString(map.get("tenant")));
                }

                if (map.containsKey("password")) {
                    newData.setKey(isPlaceHolder(map.get("password")) ? this.txtPassword.getText() : map.get("password"));
                }

                if (map.containsKey("fileWithCertAndPrivateKey")) {
                    newData.setCertificate(StringUtils.defaultString(map.get("fileWithCertAndPrivateKey")));
                }
                setValue(newData);
            } catch (JsonSyntaxException ex) {
                // ignore all json errors
            } finally {
                intermediateState.set(false);
            }
        }

        private boolean isPlaceHolder(String password) {
            return Arrays.asList("<hidden>", "<empty>").contains(password);
        }

        @Override
        protected void checkSubclass() {
            // Disable the check that prevents subclassing of SWT components
        }

        @Override
        public AuthConfiguration getValue() {
            AuthConfiguration data = new AuthConfiguration(AuthType.SERVICE_PRINCIPAL);

            data.setClient(txtClientId.getValue());
            data.setTenant(txtTenantId.getValue());
            if (radioPassword.getSelection()) {
                data.setKey(txtPassword.getValue());
            } else {
                data.setCertificate(this.txtCertificate.getValue());
            }
            data.setType(AuthType.SERVICE_PRINCIPAL);
            return data;
        }

        @Override
        public void setValue(AuthConfiguration model) {
            this.txtTenantId.setValue(StringUtils.defaultString(model.getTenant()));
            this.txtClientId.setValue(StringUtils.defaultString(model.getClient()));

            if (!StringUtils.isAllBlank(model.getCertificate(), model.getKey())) {
                if (model.getKey() != null) {
                    this.txtPassword.setValue(model.getKey());
                    this.radioPassword.setSelection(true);
                    this.radioCertificate.setSelection(false);
                } else {
                    this.txtCertificate.setText(model.getCertificate());
                    this.radioPassword.setSelection(false);
                    this.radioCertificate.setSelection(true);
                }
            }
        }

        public List<AzureValidationInfo> validateData() {
            List<AzureValidationInfo> list = new ArrayList<>();
            AzureTaskManager.getInstance().runAndWait(() -> {
                list.addAll(this.validateAllInputs());
                if (this.radioPassword.getSelection()) {
                    list.add(validateRequiredTemporarily(this.txtPassword));
                } else {
                    list.add(validateRequiredTemporarily(this.txtCertificate));
                }
            });
            return list;
        }

        private AzureValidationInfo validateRequiredTemporarily(AzureFormInput<?> input) {
            try {
                input.setRequired(true);
                return input.validateValue();
            } finally {
                input.setRequired(false);
            }
        }

        @Override
        public List<AzureFormInput<?>> getInputs() {
            return Arrays.asList(this.txtClientId, this.txtTenantId, this.txtPassword, this.txtCertificate);
        }
    }
}