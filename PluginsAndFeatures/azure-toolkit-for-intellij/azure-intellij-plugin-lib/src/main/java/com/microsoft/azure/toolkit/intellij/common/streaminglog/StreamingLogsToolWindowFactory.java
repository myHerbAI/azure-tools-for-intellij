/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.streaminglog;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.jetbrains.annotations.NotNull;

public class StreamingLogsToolWindowFactory implements ToolWindowFactory {

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setToHideOnEmptyContent(true);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return false;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(ContentManagerEvent contentManagerEvent) {
                final String displayName = contentManagerEvent.getContent().getDisplayName();
                StreamingLogsToolWindowManager.getInstance().removeConsoleViewName(displayName);
            }

            @Override
            public void contentRemoveQuery(@NotNull ContentManagerEvent event) {
                final Disposable disposable = event.getContent().getDisposer();
                if (disposable instanceof StreamingLogsConsoleView && !((StreamingLogsConsoleView) disposable).isActive()) {
                    return;
                }
                final String displayName = event.getContent().getDisplayName();
                final boolean canClose = AzureMessager.getMessager().confirm(AzureString.format(
                        "This will stop streaming log of \"{0}\", are you sure to do this?", displayName));
                if (!canClose) {
                    event.consume();
                }
            }

        });
    }
}
