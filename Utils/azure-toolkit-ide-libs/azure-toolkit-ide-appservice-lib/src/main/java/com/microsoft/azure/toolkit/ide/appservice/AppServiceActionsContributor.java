/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class AppServiceActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> START_STREAM_LOG = Action.Id.of("user/appservice.open_log_stream.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> STOP_STREAM_LOG = Action.Id.of("user/appservice.close_log_stream.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> OPEN_LOGS_IN_MONITOR = Action.Id.of("user/appservice.open_azure_monitor.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> REFRESH_DEPLOYMENT_SLOTS = Action.Id.of("user/appservice.refresh_deployments.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> OPEN_IN_BROWSER = Action.Id.of("user/webapp.open_in_browser.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> SSH_INTO_WEBAPP = Action.Id.of("user/webapp.connect_ssh.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> PROFILE_FLIGHT_RECORD = Action.Id.of("user/webapp.profile_flight_recorder.app");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_IN_BROWSER)
            .withLabel("Open In Browser")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus(true).isConnected())
            .withHandler(s -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://" + s.getHostName()))
            .register(am);

        new Action<>(START_STREAM_LOG)
            .withLabel("Start Streaming Logs")
            .withIcon(AzureIcons.Action.LOG.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);

        new Action<>(STOP_STREAM_LOG)
            .withLabel("Stop Streaming Logs")
            .withIcon(AzureIcons.Action.LOG.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);

        new Action<>(PROFILE_FLIGHT_RECORD)
            .withLabel("Profile Flight Recorder")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);

        new Action<>(SSH_INTO_WEBAPP)
            .withLabel("SSH into Web App")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);

        new Action<>(REFRESH_DEPLOYMENT_SLOTS)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .withHandler(app -> AzureEventBus.emit("appservice.slot.refresh", app))
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .register(am);

        new Action<>(OPEN_LOGS_IN_MONITOR)
            .withLabel("Open Logs with Azure Monitor")
            .withIcon(AzureIcons.Common.AZURE_MONITOR.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzResource, Object> startCondition = (r, e) -> r instanceof AppServiceAppBase<?, ?, ?> &&
            StringUtils.equals(r.getStatus(), AzResource.Status.STOPPED);
        final BiConsumer<AzResource, Object> startHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).start();
        am.registerHandler(ResourceCommonActionsContributor.START, startCondition, startHandler);

        final BiPredicate<AzResource, Object> stopCondition = (r, e) -> r instanceof AppServiceAppBase<?, ?, ?> &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource, Object> stopHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).stop();
        am.registerHandler(ResourceCommonActionsContributor.STOP, stopCondition, stopHandler);

        final BiPredicate<AzResource, Object> restartCondition = (r, e) -> r instanceof AppServiceAppBase<?, ?, ?> &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource, Object> restartHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).restart();
        am.registerHandler(ResourceCommonActionsContributor.RESTART, restartCondition, restartHandler);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
