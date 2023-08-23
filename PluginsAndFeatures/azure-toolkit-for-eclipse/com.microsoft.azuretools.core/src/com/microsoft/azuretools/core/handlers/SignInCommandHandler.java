/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.handlers;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.core.ui.DeviceLoginWindow;
import com.microsoft.azuretools.core.ui.SignInWindow;
import com.microsoft.azuretools.core.ui.login.ServicePrincipalLoginDialog;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SignInCommandHandler extends AzureAbstractHandler {
	public static final String MUST_SELECT_SUBSCRIPTION = "Please select at least one subscription first (Tools -> Azure -> Select Subscriptions)";

	@Override
	public Object onExecute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		authActionPerformed(window.getShell());

		return null;
	}

	public static void doSignIn(Shell shell) {
		requireSignedIn(shell, (a) -> {
		});
	}

	private static boolean showYesNoDialog(Shell shell, String title, String message) {
		MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		messageBox.setMessage(message);
		messageBox.setText(title);
		return SWT.YES == messageBox.open();
	}

	private void authActionPerformed(Shell shell) {
		final AzureAccount az = Azure.az(AzureAccount.class);
		if (az.isLoggedIn()) {
			final Account account = az.account();
			final AuthType authType = account.getType();
			final String warningMessage = String.format("Signed in as \"%s\" with %s", account.getUsername(), authType.getLabel());
			final String additionalMsg = authType == AuthType.AZURE_CLI ? "(This will not sign you out from Azure CLI)" : "";
			final String msg = String.format("%s\nDo you really want to sign out? %s", warningMessage, additionalMsg);
			final boolean toLogout = showYesNoDialog(shell, "Azure Sign Out", msg);
			if (toLogout) {
				az.logout();
			}
		} else {
			login(shell, (a) -> {
			});
		}
	}

	@AzureOperation(name = "internal/account.sign_in")
	private static void login(Shell shell, Consumer<IAccount> callback) {
		final AzureTaskManager manager = AzureTaskManager.getInstance();
		manager.runLater(() -> {
			final AuthConfiguration auth = promptForAuthConfiguration(shell);
			if (Objects.isNull(auth)) {
				return;
			}
			final DeviceLoginWindow[] dcWindow = new DeviceLoginWindow[1];
			if (auth.getType() == AuthType.DEVICE_CODE) {
				dcWindow[0] = setupDeviceCodeAuth(shell, auth);
			}
			final AzureString title = OperationBundle.description("account.sign_in");
			final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
				try {
					final Account account = Azure.az(AzureAccount.class).login(auth, true);
					if (account.isLoggedIn()) {
						SelectSubscriptionsCommandHandler.onSelectSubscriptions(shell);
                        manager.runOnPooledThread(() -> callback.accept(account));
					}
				} catch (final Throwable t) {
					final Throwable cause = ExceptionUtils.getRootCause(t);
					Optional.ofNullable(dcWindow[0]).ifPresent(w -> manager.runLater(w::cancelPressed));
					if (!(cause instanceof InterruptedException)) {
						throw t;
					}
				}
			});
			manager.runInBackground(task);
		});
	}

	private static DeviceLoginWindow setupDeviceCodeAuth(Shell shell, AuthConfiguration auth) {
		final AzureTaskManager manager = AzureTaskManager.getInstance();
		auth.setExecutorService(Executors.newFixedThreadPool(1));
		final DeviceLoginWindow dcWindow = new DeviceLoginWindow(shell);
		dcWindow.setDoOnCancel(() -> {
			if (!Azure.az(AzureAccount.class).isLoggedIn()) {
				auth.getExecutorService().shutdownNow();
			}
		});
		auth.setDeviceCodeConsumer(info -> manager.runLater(() -> dcWindow.show(info)));
		auth.setDoAfterLogin(() -> manager.runLater(dcWindow::close, AzureTask.Modality.ANY));
		return dcWindow;
	}

	private static AuthConfiguration promptForAuthConfiguration(Shell shell) {
		final SignInWindow dialog = new SignInWindow(shell);
		dialog.create();
		if (dialog.open() != Window.OK) {
			return null;
		}

		AuthConfiguration config = new AuthConfiguration(dialog.getData());
		if (config.getType() == AuthType.SERVICE_PRINCIPAL) {
			final ServicePrincipalLoginDialog spDialog = new ServicePrincipalLoginDialog(shell);
			spDialog.create();
			if (spDialog.open() != Window.OK) {
				return null;
			}
			config = spDialog.getValue();
		}
		return config;
	}

	public static void requireSignedIn(Shell project, Consumer<IAccount> consumer) {
		if (IdeAzureAccount.getInstance().isLoggedIn()) {
            final Account account = Azure.az(AzureAccount.class).account();
            AzureTaskManager.getInstance().runOnPooledThread(() -> consumer.accept(account));
		} else {
			login(project, consumer);
		}
	}
}
