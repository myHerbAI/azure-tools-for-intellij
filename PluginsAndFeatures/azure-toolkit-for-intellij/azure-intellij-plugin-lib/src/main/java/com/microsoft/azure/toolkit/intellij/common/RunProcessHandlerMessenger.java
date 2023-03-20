/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.execution.process.ProcessOutputType;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RunProcessHandlerMessenger extends IntellijAzureMessager {
    private final RunProcessHandler handler;

    @Override
    public boolean show(IAzureMessage raw) {
        if (raw.getType() == IAzureMessage.Type.INFO || raw.getType() == IAzureMessage.Type.WARNING) {
            handler.setText(raw.getMessage().toString());
            return true;
        } else if (raw.getType() == IAzureMessage.Type.SUCCESS) {
            handler.println(raw.getMessage().toString(), ProcessOutputType.SYSTEM);
        } else if (raw.getType() == IAzureMessage.Type.ERROR) {
            handler.println(raw.getContent(), ProcessOutputType.STDERR);
        }
        return super.show(raw);
    }
}