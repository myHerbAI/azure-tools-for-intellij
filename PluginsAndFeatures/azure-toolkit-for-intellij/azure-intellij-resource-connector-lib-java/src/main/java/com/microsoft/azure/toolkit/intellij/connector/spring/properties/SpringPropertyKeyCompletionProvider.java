/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring.properties;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class SpringPropertyKeyCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<? extends SpringSupported<?>> definitions = ResourceManager
        .getDefinitions(ResourceDefinition.RESOURCE).stream()
        .filter(d -> d instanceof SpringSupported)
        .map(d -> (SpringSupported<?>) d).toList();

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final PsiFile file = parameters.getOriginalFile();
        definitions.stream().flatMap(definition ->
            definition.getSpringProperties().stream().filter(p -> !p.getKey().trim().startsWith("#")).map(p -> LookupElementBuilder
                .create(definition.getName(), p.getKey())
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withPsiElement(getPropertyField(definition.getSpringPropertyFields().get(p.getKey()), file))
                .withBoldness(true)
                .withInsertHandler(new PropertyKeyInsertHandler())
                .withTypeText("Property Key")
                .withTailText(String.format(" (%s)", definition.getTitle())))
        ).forEach(result::addElement);
        result.addLookupAdvertisement("Press enter and select a Azure Storage Account to connect");
    }

    @Nullable
    private static PsiElement getPropertyField(final String fullQualifiedFieldName, final PsiFile file) {
        if (StringUtils.isNotBlank(fullQualifiedFieldName)) {
            final String[] parts = fullQualifiedFieldName.split("#");
            final PsiClass psiClass = JavaPsiFacade.getInstance(file.getProject()).findClass(parts[0], file.getResolveScope());
            return Optional.ofNullable(psiClass).map(c -> c.findFieldByName(parts[1], true)).orElse(null);
        }
        return null;
    }

    @RequiredArgsConstructor
    private static class PropertyKeyInsertHandler implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @Nonnull LookupElement item) {
            final CaretModel caretModel = context.getEditor().getCaretModel();
            context.getDocument().insertString(caretModel.getOffset(), "=");
            context.getEditor().getCaretModel().moveToOffset(caretModel.getOffset() + 1);
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
        }
    }
}
