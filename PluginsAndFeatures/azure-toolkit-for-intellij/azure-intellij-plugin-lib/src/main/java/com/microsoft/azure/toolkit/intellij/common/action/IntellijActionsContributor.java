/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.FUSEventSource;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;
import java.util.function.BiConsumer;

public class IntellijActionsContributor implements IActionsContributor {
    public static final Action.Id<Object> TRY_ULTIMATE = Action.Id.of("database.try_ultimate");
    private static final String IDE_DOWNLOAD_URL = "https://www.jetbrains.com/idea/download/";

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.OPEN_URL, Objects::nonNull, IntellijActionsContributor::browseUrl);
        am.<AzResourceBase, AnActionEvent>registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES,
            (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()),
            (s, e) -> IntellijShowPropertiesViewAction.showPropertyView(s, Objects.requireNonNull(e.getProject())));

        final BiConsumer<Object, AnActionEvent> highlightResource = (r, e) -> {
            AzureEventBus.emit("azure.explorer.highlight_resource", r);
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER).handle(null, e);
        };
        am.registerHandler(ResourceCommonActionsContributor.HIGHLIGHT_RESOURCE_IN_EXPLORER, (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()), highlightResource);
    }


    @AzureOperation(name = "resource.open_url.url", params = {"u"}, type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private static void browseUrl(String u) {
        BrowserUtil.browse(u);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> handler = (r, e) -> FUSEventSource.NOTIFICATION.openDownloadPageAndLog(e.getProject(), IDE_DOWNLOAD_URL);
        final ActionView.Builder view = new ActionView.Builder(IdeBundle.message("plugins.advertiser.action.try.ultimate", "IntelliJ IDEA Ultimate"));
        final Action<Object> tryUltimate = new Action<>(TRY_ULTIMATE, handler, view);
        am.registerAction(TRY_ULTIMATE, tryUltimate);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1; //after azure resource common actions registered
    }
}
