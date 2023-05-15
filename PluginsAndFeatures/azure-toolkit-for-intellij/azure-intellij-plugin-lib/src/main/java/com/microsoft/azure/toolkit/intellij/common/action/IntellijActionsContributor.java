/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.FUSEventSource;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Objects;
import java.util.function.BiConsumer;

public class IntellijActionsContributor implements IActionsContributor {
    public static final Action.Id<Object> TRY_ULTIMATE = Action.Id.of("user/database.try_ultimate");
    private static final String IDE_DOWNLOAD_URL = "https://www.jetbrains.com/idea/download/";

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.OPEN_URL, Objects::nonNull, IntellijActionsContributor::browseUrl);
        am.<AzResource, AnActionEvent>registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES,
            (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()),
            (s, e) -> IntellijShowPropertiesViewAction.showPropertyView(s, Objects.requireNonNull(e.getProject())));

        final BiConsumer<Object, AnActionEvent> highlightResource = (r, e) -> {
            AzureEventBus.emit("azure.explorer.highlight_resource", r);
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER).handle(null, e);
        };
        am.registerHandler(ResourceCommonActionsContributor.HIGHLIGHT_RESOURCE_IN_EXPLORER, (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()), highlightResource);

        final AzureTaskManager tm = AzureTaskManager.getInstance();
        am.registerHandler(ResourceCommonActionsContributor.RESTART_IDE, (s, e) -> tm.runLater(() -> {
            final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
            app.restart();
        }));
    }

    @AzureOperation(name = "boundary/resource.open_url.url", params = {"u"})
    private static void browseUrl(String u) {
        BrowserUtil.browse(u);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(TRY_ULTIMATE)
            .withLabel("Try IntelliJ IDEA Ultimate")
            .withHandler((r, e) -> FUSEventSource.NOTIFICATION.openDownloadPageAndLog(((AnActionEvent) e).getProject(), IDE_DOWNLOAD_URL))
            .register(am);

        new Action<>(ResourceCommonActionsContributor.REVEAL_FILE)
            .withLabel(RevealFileAction.getActionName())
            .withHandler(VirtualFileActions::revealInExplorer)
            .register(am);

        new Action<>(ResourceCommonActionsContributor.OPEN_FILE)
            .withLabel("Open In Editor")
            .withHandler((file, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final FileEditorManager fileEditorManager = FileEditorManager.getInstance(((AnActionEvent) e).getProject());
                final VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
                VirtualFileActions.openFileInEditor(virtualFile, (a) -> false, () -> {
                }, fileEditorManager);
            }))
            .register(am);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1; //after azure resource common actions registered
    }
}
