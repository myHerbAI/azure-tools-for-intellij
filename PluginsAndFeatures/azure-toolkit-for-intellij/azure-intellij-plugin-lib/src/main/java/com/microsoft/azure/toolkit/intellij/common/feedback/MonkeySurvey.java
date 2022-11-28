/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFileType;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;

public class MonkeySurvey {
    public static final Key<String> SURVEY_URL = Key.create("survey_url");
    public static final Key<Integer> ORIGINAL_CORE = Key.create("original_core");
    @Nonnull
    private static final String url = "https://www.surveymonkey.com/r/PNB5NBL?mode=simple";

    public static void openInIDE(@Nonnull Project project) {
        openInIDE(project, -1);
    }

    public static void openInIDE(@Nonnull Project project, int score) {
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager == null) {
            return;
        }
        LightVirtualFile survey = searchExistingFile(manager);
        if (survey == null) {
            final String title = "Provide Feedback";
            survey = new LightVirtualFile(title);
            survey.putUserData(SURVEY_URL, url);
            survey.putUserData(ORIGINAL_CORE, score);
            survey.setFileType(new AzureFileType(MonkeySurveyEditorProvider.TYPE, IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)));
        }
        final LightVirtualFile finalItemVirtualFile = survey;
        AzureTaskManager.getInstance().runLater(() -> manager.openFile(finalItemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/));
    }

    private static LightVirtualFile searchExistingFile(FileEditorManager fileEditorManager) {
        LightVirtualFile virtualFile = null;
        for (final VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            final String surveyUrl = editedFile.getUserData(SURVEY_URL);
            if (surveyUrl != null && surveyUrl.equals(url)) {
                virtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        return virtualFile;
    }
}