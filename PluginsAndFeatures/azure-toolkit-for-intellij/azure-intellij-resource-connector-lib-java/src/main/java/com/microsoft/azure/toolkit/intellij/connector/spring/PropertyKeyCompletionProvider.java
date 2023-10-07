/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

public class PropertyKeyCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<? extends SpringSupported<?>> definitions = ResourceManager
        .getDefinitions(ResourceDefinition.RESOURCE).stream()
        .filter(d -> d instanceof SpringSupported)
        .map(d -> (SpringSupported<?>) d).toList();

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final String originalPrefix = result.getPrefixMatcher().getPrefix();
        final String prefix = parameters.getPosition().getParent().getLastChild().getText().replace("IntellijIdeaRulezzz", StringUtils.EMPTY);
        if (!StringUtils.equals(originalPrefix, prefix)) {
            result = result.withPrefixMatcher(prefix);
        }
        definitions.stream().flatMap(definition ->
            definition.getSpringProperties().stream().map(p -> LookupElementBuilder
                .create(definition.getName(), p.getKey())
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withBoldness(true)
                .withInsertHandler(new PropertyKeyInsertHandler(p.getKey()))
                .withTypeText("String")
                .withTailText(String.format(" (%s)", definition.getTitle())))
        ).forEach(result::addElement);
    }

    @RequiredArgsConstructor
    private static class PropertyKeyInsertHandler implements InsertHandler<LookupElement> {
        private final String key;

        @Override
        public void handleInsert(@NotNull InsertionContext context, @Nonnull LookupElement item) {
            final CaretModel caretModel = context.getEditor().getCaretModel();
            context.getDocument().insertString(caretModel.getOffset(), "=");
            context.getEditor().getCaretModel().moveToOffset(caretModel.getOffset() + 1);
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
        }
    }
}
