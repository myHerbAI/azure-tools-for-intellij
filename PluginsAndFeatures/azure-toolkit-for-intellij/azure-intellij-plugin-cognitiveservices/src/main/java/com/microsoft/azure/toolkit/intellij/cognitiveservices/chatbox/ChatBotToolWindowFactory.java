// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import lombok.Getter;

import javax.annotation.Nonnull;

public class ChatBotToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public boolean shouldBeAvailable(@Nonnull final Project project) {
        return true;
    }

    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow window) {
        final ContentManager contentManager = window.getContentManager();
        final ChatBox chatBox = new ChatBox();
        final ContentFactory contentFactory = ContentFactory.getInstance();
        final Content content = contentFactory.createContent(chatBox.getContentPanel(), "", false);
        contentManager.addContent(content);
        window.getComponent().putClientProperty("ChatBox", chatBox);
    }
}
