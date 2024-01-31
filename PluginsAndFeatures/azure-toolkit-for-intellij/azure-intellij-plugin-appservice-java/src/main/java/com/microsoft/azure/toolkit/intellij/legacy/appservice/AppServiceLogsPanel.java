/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.icons.AllIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureIntegerInput;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.Getter;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Getter
public class AppServiceLogsPanel implements AzureFormPanel<DiagnosticConfig> {
	private JPanel pnlRoot;
	private JRadioButton rdoDisableDetailError;
	private JRadioButton rdoEnableDetailError;
	private JRadioButton rdoDisableFailedRequest;
	private JRadioButton rdoEnableFailedRequest;
	private JRadioButton rdoDisableApplicationLog;
	private JRadioButton rdoEnableApplicationLog;
	private JLabel lblWebServerLog;
	private JRadioButton rdoDisableWebServerLog;
	private JRadioButton rdoEnableWebServerLog;
	private JPanel pnlWebServerLog;
	private JLabel lblQuota;
	private JLabel lblRetention;
	private JLabel lblDetailedErrorMessage;
	private JLabel lblFailedRequest;
	private JPanel pnlApplicationLog;
	private AzureIntegerInput txtQuota;
	private AzureIntegerInput txtRetention;
	private LogLevelComboBox cbLogLevel;
	private JLabel lblApplicationLog;
	private JPanel pnlWebServer;
	private JPanel pnlApplication;

	public AppServiceLogsPanel() {
		super();
		init();
	}

	public void setApplicationLogVisible(boolean visible) {
		pnlApplication.setVisible(visible);
		rdoEnableApplicationLog.setSelected(visible);
	}

	public void setWebServerLogVisible(boolean visible) {
		pnlWebServer.setVisible(visible);
		txtQuota.setRequired(visible);
		txtQuota.revalidate();
	}

	@Override
	public DiagnosticConfig getValue() {
		return DiagnosticConfig.builder()
		                       .enableWebServerLogging(rdoEnableWebServerLog.isSelected() && pnlWebServer.isVisible())
		                       .webServerLogQuota(txtQuota.getValue())
		                       .webServerRetentionPeriod(txtRetention.getValue())
		                       .enableDetailedErrorMessage(rdoEnableDetailError.isSelected())
		                       .enableFailedRequestTracing(rdoEnableFailedRequest.isSelected())
		                       .enableApplicationLog(rdoEnableApplicationLog.isSelected() && pnlApplication.isVisible())
		                       .applicationLogLevel(cbLogLevel.getValue()).build();
	}

	@Override
	public void setValue(final DiagnosticConfig config) {
		if (pnlWebServer.isVisible()) {
			rdoEnableWebServerLog.setSelected(config.isEnableWebServerLogging());
			txtQuota.setValue(config.getWebServerLogQuota());
			txtRetention.setValue(config.getWebServerRetentionPeriod());
			rdoEnableDetailError.setSelected(config.isEnableDetailedErrorMessage());
			rdoEnableFailedRequest.setSelected(config.isEnableFailedRequestTracing());
		}
		if (pnlApplication.isVisible()) {
			rdoEnableApplicationLog.setSelected(config.isEnableApplicationLog());
			cbLogLevel.setSelectedItem(config.getApplicationLogLevel());
		}
	}

	@Override
	public List<AzureFormInput<?>> getInputs() {
		return Arrays.asList(cbLogLevel, txtQuota, txtRetention);
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
		cbLogLevel = new LogLevelComboBox();

		txtQuota = new AzureIntegerInput();
		txtQuota.setMinValue(25);
		txtQuota.setMaxValue(100);

		txtRetention = new AzureIntegerInput();
		txtRetention.setMinValue(0);
		txtRetention.setMaxValue(99999);
		txtRetention.setRequired(false);
	}

	private void init() {
		final ButtonGroup webServerGroup = new ButtonGroup();
		webServerGroup.add(rdoEnableWebServerLog);
		webServerGroup.add(rdoDisableWebServerLog);
		rdoEnableWebServerLog.addItemListener(e -> setWebServerLogEnabled(rdoEnableWebServerLog.isSelected()));
		rdoDisableWebServerLog.addItemListener(e -> setWebServerLogEnabled(rdoEnableWebServerLog.isSelected()));

		final ButtonGroup detailedErrorMessageGroup = new ButtonGroup();
		detailedErrorMessageGroup.add(rdoEnableDetailError);
		detailedErrorMessageGroup.add(rdoDisableDetailError);

		final ButtonGroup failedRequestGroup = new ButtonGroup();
		failedRequestGroup.add(rdoEnableFailedRequest);
		failedRequestGroup.add(rdoDisableFailedRequest);


		final ButtonGroup applicationLogGroup = new ButtonGroup();
		applicationLogGroup.add(rdoEnableApplicationLog);
		applicationLogGroup.add(rdoDisableApplicationLog);
		rdoEnableApplicationLog.addItemListener(e -> setApplicationLogEnabled(rdoEnableApplicationLog.isSelected()));
		rdoDisableApplicationLog.addItemListener(e -> setApplicationLogEnabled(rdoEnableApplicationLog.isSelected()));

		this.lblWebServerLog.setIcon(AllIcons.General.ContextHelp);
		this.lblQuota.setIcon(AllIcons.General.ContextHelp);
		this.lblRetention.setIcon(AllIcons.General.ContextHelp);
		this.lblApplicationLog.setIcon(AllIcons.General.ContextHelp);
	}

	public void setWebServerLogEnabled(boolean enable) {
//		rdoEnableWebServerLog.setSelected(enable);
//		rdoDisableWebServerLog.setSelected(!enable);
		pnlWebServerLog.setVisible(enable);
		txtQuota.setRequired(enable);
	}

	public void setApplicationLogEnabled(boolean enable) {
		pnlApplicationLog.setVisible(enable);
	}

	@Override
	public void setVisible(boolean visible) {
		this.pnlRoot.setVisible(visible);
	}
}
