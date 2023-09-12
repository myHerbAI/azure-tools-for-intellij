/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.eclipse.appservice.property.AppServicePropertyEditorInput;
import com.microsoft.azure.toolkit.eclipse.function.launch.deploy.DeployAzureFunctionAction;
import com.microsoft.azure.toolkit.eclipse.functionapp.creation.CreateFunctionAppHandler;
import com.microsoft.azure.toolkit.eclipse.functionapp.logstreaming.FunctionAppLogStreamingHandler;
import com.microsoft.azure.toolkit.eclipse.functionapp.property.FunctionAppPropertyEditor;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.model.TriggerRequest;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.utils.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class EclipseFunctionAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = FunctionAppActionsContributor.INITIALIZE_ORDER + 1;

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, Object> createCondition = (r, e) -> r instanceof AzureFunctions;
        final BiConsumer<Object, Object> createHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> CreateFunctionAppHandler.create());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition, createHandler);

        final BiPredicate<AzResource, Object> isFunctionApp = (r, e) -> r instanceof FunctionApp;
        final BiConsumer<AzResource, Object> openWebAppPropertyViewHandler = (c, e) -> AzureTaskManager
                .getInstance().runLater(() -> {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    AppServicePropertyEditorInput input = new AppServicePropertyEditorInput(c.getId());
                    IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(FunctionAppPropertyEditor.ID);
                    openEditor(input, descriptor);
                });
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isFunctionApp, openWebAppPropertyViewHandler);

        final BiConsumer<AzResource, Object> deployHandler = (c, e) -> AzureTaskManager.getInstance().runLater(() -> {
            try {
                DeployAzureFunctionAction.deployFunctionAppToAzure((FunctionApp) c);
            } catch (CoreException exception) {
                AzureMessager.getMessager().error(exception);
            }
        });
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, (r, e) -> r instanceof FunctionApp, deployHandler);

        final BiPredicate<AppServiceAppBase<?, ?, ?>, Object> logStreamingPredicate = (r,
                e) -> r instanceof FunctionAppBase<?, ?, ?>;
        final BiConsumer<AppServiceAppBase<?, ?, ?>, Object> startLogStreamingHandler = (c,
                e) -> FunctionAppLogStreamingHandler.startLogStreaming((FunctionAppBase<?, ?, ?>) c);
        am.registerHandler(AppServiceActionsContributor.START_STREAM_LOG, logStreamingPredicate,
                startLogStreamingHandler);

        final BiConsumer<AppServiceAppBase<?, ?, ?>, Object> stopLogStreamingHandler = (c,
                e) -> FunctionAppLogStreamingHandler.stopLogStreaming((FunctionAppBase<?, ?, ?>) c);
        am.registerHandler(AppServiceActionsContributor.STOP_STREAM_LOG, logStreamingPredicate,
                stopLogStreamingHandler);

        // todo: Remove duplicated codes with IntelliJ
        final BiPredicate<FunctionEntity, Object> triggerPredicate = (r, e) -> r instanceof FunctionEntity;
        final BiConsumer<FunctionEntity, Object> triggerFunctionHandler = (entity, e) -> {
            final String functionId = Optional.ofNullable(entity.getFunctionAppId())
                    .orElseGet(() -> ResourceId.fromString(entity.getTriggerId()).parent().id());
            final FunctionApp functionApp = Azure.az(AzureFunctions.class).functionApp(functionId);
            final String triggerType = Optional.ofNullable(entity.getTrigger())
                    .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
            if (StringUtils.equalsIgnoreCase(triggerType, "timertrigger")) {
                functionApp.triggerFunction(entity.getName(), new Object());
            } else {
                final String input = AzureTaskManager.getInstance().runAndWait(() -> {
                    final InputDialog inputDialog = new InputDialog(PluginUtil.getParentShell(), String.format("Trigger function %s", entity.getName()),
                            "Please set the input value: ", null, null);
                    return inputDialog.open() == Window.OK ? inputDialog.getValue() : null;
                }).join();
                if (input != null) {
                    functionApp.triggerFunction(entity.getName(), new TriggerRequest(input));
                }
            }
        };
        am.registerHandler(FunctionAppActionsContributor.TRIGGER_FUNCTION, triggerPredicate, triggerFunctionHandler);
    }

    // todo: remove duplicated with EclipseWebAppActionsContributor
    private void openEditor(IEditorInput input, IEditorDescriptor descriptor) {
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
            AzureMessager.getMessager().error(e, "Unable to open editor view");
        }
    }

    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
