/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice.actions;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class TriggerFunctionAction {

    private static final String REQUEST_TEMPLATE =
            "### Please refer https://docs.microsoft.com/en-us/azure/azure-functions/functions-manually-run-non-http for details\n" +
                    "POST https://%s/admin/functions/%s\n" +
                    "Content-Type: application/json\n" +
                    "x-functions-key: %s\n" +
                    "\n" +
                    "%s";

    private static final String HTTP_TRIGGER_REQUEST_TEMPLATE =
            "### Please refer https://docs.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook-trigger for details\n" +
                    "POST https://%s/api/%s?code=%s\n" +
                    "Content-Type: application/json\n" +
                    "\n" +
                    "{}";
    private static final String EMPTY_CONTENT = "{}";
    private static final String INPUT_CONTENT = "{ \"input\" : \"test\" }";

    @AzureOperation(name = "boundary/function.trigger_with_http_client.function", params = {"functionEntity.getName()"})
    public static void triggerFunction(@Nonnull FunctionEntity functionEntity, @Nonnull Project project) {
        try {
            final ResourceId resourceId = ResourceId.fromString(functionEntity.getTriggerId());
            final String fileName = PathUtil.makeFileName(String.format("%s-%s", resourceId.parent().name(), resourceId.name()), "http");
            final VirtualFile file = ScratchFileService.getInstance().findFile(ScratchRootType.getInstance(), fileName,
                    ScratchFileService.Option.create_if_missing);
            final FileEditor[] fileEditors = FileEditorManager.getInstance(project).openFile(file, true);
            final TextEditor textEditor = Arrays.stream(fileEditors)
                    .filter(editor -> editor instanceof TextEditor && StringUtils.equals(editor.getFile().getPath(), file.getPath()))
                    .map(editor -> (TextEditor) editor)
                    .findFirst().orElse(null);
            final Document document = textEditor.getEditor().getDocument();
            if (StringUtils.isEmpty(document.getText())) {
                WriteAction.run(() -> document.setText(getRequestContent(functionEntity)));
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static String getRequestContent(FunctionEntity functionEntity) {
        final FunctionApp functionApp = Azure.az(AzureFunctions.class).functionApp(functionEntity.getFunctionAppId());
        final String triggerType = Optional.ofNullable(functionEntity.getTrigger())
                .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
        if (StringUtils.equalsIgnoreCase(triggerType, "httpTrigger")) {
            return String.format(HTTP_TRIGGER_REQUEST_TEMPLATE, functionApp.getHostName(), functionEntity.getName(), functionApp.getMasterKey());
        } else if (StringUtils.equalsIgnoreCase(triggerType, "timerTrigger")) {
            return String.format(REQUEST_TEMPLATE, functionApp.getHostName(), functionEntity.getName(), functionApp.getMasterKey(), EMPTY_CONTENT);
        } else {
            return String.format(REQUEST_TEMPLATE, functionApp.getHostName(), functionEntity.getName(), functionApp.getMasterKey(), INPUT_CONTENT);
        }
    }
}
