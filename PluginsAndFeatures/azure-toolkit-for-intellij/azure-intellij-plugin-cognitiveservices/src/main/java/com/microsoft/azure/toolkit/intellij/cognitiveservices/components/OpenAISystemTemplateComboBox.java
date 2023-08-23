/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.service.SystemMessageTemplateService;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class OpenAISystemTemplateComboBox extends AzureComboBox<SystemMessage> {
    public OpenAISystemTemplateComboBox() {
        super(true);
    }

    @Nonnull
    @Override
    protected List<? extends SystemMessage> loadItems() {
        return SystemMessageTemplateService.loadTemplates();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof SystemMessage ? ((SystemMessage) item).getName() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
