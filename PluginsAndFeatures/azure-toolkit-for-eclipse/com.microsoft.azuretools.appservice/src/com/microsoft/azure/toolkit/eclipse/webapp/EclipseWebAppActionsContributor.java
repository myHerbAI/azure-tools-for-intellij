/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import com.microsoft.azure.toolkit.eclipse.appservice.property.AppServicePropertyEditorInput;
import com.microsoft.azure.toolkit.eclipse.webapp.handlers.DownloadAppServiceFileAction;
import com.microsoft.azure.toolkit.eclipse.webapp.handlers.WebAppLogStreamingHandler;
import com.microsoft.azure.toolkit.eclipse.webapp.property.DeploymentSlotEditor;
import com.microsoft.azure.toolkit.eclipse.webapp.property.WebAppPropertyEditor;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.webapp.WebAppActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azureexplorer.helpers.EditorType;

public class EclipseWebAppActionsContributor implements IActionsContributor {
	public static final int INITIALIZE_ORDER = WebAppActionsContributor.INITIALIZE_ORDER + 1;

	private static final String UNABLE_TO_OPEN_EXPLORER = "Unable to open explorer";

	@Override
	public void registerActions(AzureActionManager am) {
		final BiConsumer<AppServiceFile, Object> downloadHandler = (file, e) -> AzureTaskManager.getInstance()
				.runLater(() -> DownloadAppServiceFileAction.downloadAppServiceFile(file));
		new Action<>(AppServiceFileActionsContributor.APP_SERVICE_FILE_DOWNLOAD).withLabel("Download")
				.withIcon("/icons/action/refresh").withIdParam(s -> s.getName())
				.enableWhen(s -> s instanceof AppServiceFile).withHandler(downloadHandler).register(am);
	}

	@Override
	public void registerHandlers(AzureActionManager am) {
		final BiPredicate<AzResource, Object> isWebApp = (r, e) -> r instanceof WebApp;
		final BiConsumer<AzResource, Object> openWebAppPropertyViewHandler = (c, e) -> AzureTaskManager
				.getInstance().runLater(() -> {
					IWorkbench workbench = PlatformUI.getWorkbench();
					AppServicePropertyEditorInput input = new AppServicePropertyEditorInput(c.getId());
					IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(WebAppPropertyEditor.ID);
					openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
				});
		am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isWebApp, openWebAppPropertyViewHandler);

		final BiPredicate<AzResource, Object> isWebAppSlot = (r, e) -> r instanceof WebAppDeploymentSlot;
		final BiConsumer<AzResource, Object> openWebAppSlotPropertyViewHandler = (c, e) -> AzureTaskManager
				.getInstance().runLater(() -> {
					IWorkbench workbench = PlatformUI.getWorkbench();
					AppServicePropertyEditorInput input = new AppServicePropertyEditorInput(c.getId());
					IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(DeploymentSlotEditor.ID);
					openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
				});
		am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isWebAppSlot,
				openWebAppSlotPropertyViewHandler);

		final BiConsumer<AzResource, Object> deployWebAppHandler = (c, e) -> AzureTaskManager.getInstance()
				.runLater(() -> {
					try {
						Command deployCommand = ((ICommandService) PlatformUI.getWorkbench()
								.getService(ICommandService.class))
										.getCommand("com.microsoft.azuretools.appservice.commands.deployToAzure");
						deployCommand.execute(
								new ExecutionEvent(null, Collections.singletonMap("resourceId", c.id()), c, null));
					} catch (ExecutionException | NotHandledException exception) {
						AzureTaskManager.getInstance().runLater(() -> AzureMessager.getMessager()
								.error(AzureString.format("Failed to deploy web app, %s", exception.getMessage())));
					}
				});
		am.registerHandler(ResourceCommonActionsContributor.DEPLOY, (r, e) -> r instanceof WebApp, deployWebAppHandler);

		final BiPredicate<Object, Object> createCondition = (r, e) -> r instanceof AzureWebApp;
		final BiConsumer<Object, Object> createHandler = (c, e) -> AzureTaskManager.getInstance().runLater(() -> {
			try {
				((IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class))
						.executeCommand("com.microsoft.azuretools.appservice.commands.createWebApp", null);
			} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException exception) {
				AzureTaskManager.getInstance().runLater(() -> AzureMessager.getMessager()
						.error(AzureString.format("Failed to create web app, %s", exception.getMessage())));
			}
		});
		am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition, createHandler);

		final BiPredicate<AppServiceAppBase<?, ?, ?>, Object> logStreamingPredicate = (r,
				e) -> r instanceof WebAppBase<?, ?, ?>;
		final BiConsumer<AppServiceAppBase<?, ?, ?>, Object> startLogStreamingHandler = (c,
				e) -> WebAppLogStreamingHandler.startLogStreaming((WebAppBase<?, ?, ?>) c);
		am.registerHandler(AppServiceActionsContributor.START_STREAM_LOG, logStreamingPredicate,
				startLogStreamingHandler);

		final BiConsumer<AppServiceAppBase<?, ?, ?>, Object> stopLogStreamingHandler = (c,
				e) -> WebAppLogStreamingHandler.stopLogStreaming((WebAppBase<?, ?, ?>) c);
		am.registerHandler(AppServiceActionsContributor.STOP_STREAM_LOG, logStreamingPredicate,
				stopLogStreamingHandler);
	}

	private void openEditor(EditorType type, IEditorInput input, IEditorDescriptor descriptor) {
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if (activeWorkbenchWindow == null) {
				return;
			}
			IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
			if (page == null) {
				return;
			}
			page.openEditor(input, descriptor.getId());
		} catch (Exception e) {
			AzureMessager.getMessager().error(UNABLE_TO_OPEN_EXPLORER);
		}
	}

	public int getOrder() {
		return INITIALIZE_ORDER;
	}
}
