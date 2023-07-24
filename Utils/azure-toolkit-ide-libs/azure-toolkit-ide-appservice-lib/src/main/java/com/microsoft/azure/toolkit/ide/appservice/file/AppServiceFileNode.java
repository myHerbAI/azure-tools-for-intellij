/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.file;

import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AppServiceFileNode extends Node<AppServiceFile> {
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";
    public static final String SITE_WWWROOT = "/site/wwwroot";
    public static final String LOG_FILES = "/LogFiles";

    private final AzureEventBus.EventListener listener;
    private final AppServiceAppBase<?, ?, ?> appService;

    public AppServiceFileNode(@Nonnull AppServiceFile data) {
        super(data);
        this.appService = data.getApp();

        final String actionGroupId = data.getType() == AppServiceFile.Type.DIRECTORY ?
            AppServiceFileActionsContributor.APP_SERVICE_DIRECTORY_ACTIONS : AppServiceFileActionsContributor.APP_SERVICE_FILE_ACTIONS;
        this.withActions(actionGroupId);
        if (data.getType() != AppServiceFile.Type.DIRECTORY) {
            this.onDoubleClicked(AppServiceFileActionsContributor.APP_SERVICE_FILE_VIEW);
        }
        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("resource.refreshed.resource", listener);
    }

    @Override
    public boolean hasChildren() {
        final AppServiceFile file = this.getValue();
        return file.getType() == AppServiceFile.Type.DIRECTORY;
    }

    @Override
    public List<Node<?>> buildChildren() {
        try {
            final AppServiceFile file = this.getValue();
            if (file.getType() != AppServiceFile.Type.DIRECTORY) {
                return Collections.emptyList();
            }
            if (!appService.getFormalStatus().isConnected()) {
                AzureMessager.getMessager().warning(AzureString.format("Can not list files of app service %s with status %s", appService.getName(), appService.getStatus()));
                return Collections.emptyList();
            }
            return appService.getFilesInDirectory(file.getPath()).stream()
                .sorted((first, second) -> first.getType() == second.getType() ?
                    StringUtils.compare(first.getName(), second.getName()) :
                    first.getType() == AppServiceFile.Type.DIRECTORY ? -1 : 1)
                .map(AppServiceFileNode::new)
                .collect(Collectors.toList());
        } catch (final Exception e) {
            AzureMessager.getMessager().error(e);
            return Collections.emptyList();
        }
    }

    private void onEvent(AzureEvent event) {
        final AppServiceFile file = this.getValue();
        final String type = event.getType();
        final Object source = event.getSource();
        if ((source instanceof AppServiceFile && StringUtils.equalsIgnoreCase(((AppServiceFile) source).getFullName(), file.getFullName()))) {
            this.refreshChildrenLater();
        }
    }

    @Override
    public String buildLabel() {
        return getValue().getName();
    }

    @Override
    public AzureIcon buildIcon() {
        final AppServiceFile file = this.getValue();
        final String fileIconName = file.getType() == AppServiceFile.Type.DIRECTORY ?
            StringUtils.equalsAnyIgnoreCase(file.getPath(), SITE_WWWROOT, LOG_FILES) ? "root" : "folder" :
            FilenameUtils.getExtension(file.getName());
        return AzureIcon.builder().iconPath(FILE_EXTENSION_ICON_PREFIX + fileIconName).build();
    }

    @Override
    public String buildDescription() {
        final AppServiceFile file = this.getValue();
        if (StringUtils.equalsAnyIgnoreCase(file.getPath(), SITE_WWWROOT, LOG_FILES)) {
            return String.format("Type: %s", file.getMime());
        }
        final String modifiedDateTime = formatDateTime(file.getMtime());
        return file.getType() == AppServiceFile.Type.DIRECTORY ?
            String.format("Type: %s Date modified: %s", file.getMime(), modifiedDateTime) :
            String.format("Type: %s Size: %s Date modified: %s", file.getMime(), FileUtils.byteCountToDisplaySize(file.getSize()), modifiedDateTime);
    }

    private static String formatDateTime(@Nullable final String dateTime) {
        try {
            return StringUtils.isEmpty(dateTime) ? "Unknown" : DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.parse(dateTime));
        } catch (final DateTimeException dateTimeException) {
            // swallow exception for parse datetime, return unknown in this case
            return "Unknown";
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        AzureEventBus.off("resource.refreshed.resource", listener);
    }

    public static AppServiceFile getRootFileNodeForAppService(@Nonnull AppServiceAppBase<?, ?, ?> appService) {
        final AppServiceFile appServiceFile = new AppServiceFile();
        appServiceFile.setName("Files");
        appServiceFile.setPath(SITE_WWWROOT);
        appServiceFile.setMime("inode/directory");
        appServiceFile.setApp(appService);
        return appServiceFile;
    }

    public static AppServiceFile getRootLogNodeForAppService(@Nonnull AppServiceAppBase<?, ?, ?> appService) {
        final AppServiceFile appServiceFile = new AppServiceFile();
        appServiceFile.setName("Logs");
        appServiceFile.setPath(LOG_FILES);
        appServiceFile.setMime("inode/directory");
        appServiceFile.setApp(appService);
        return appServiceFile;
    }
}
