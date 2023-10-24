/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring.properties;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class SpringYamlKeyCompletionProvider extends CompletionProvider<CompletionParameters> implements DumbAware {
    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final YAMLPsiElement yamlElement = PsiTreeUtil.getParentOfType(element, YAMLPsiElement.class);
        if (Objects.isNull(yamlElement)) {
            return;
        }
        final String currentKey = YAMLUtil.getConfigFullName(yamlElement);
        final List<ResourceDefinition<?>> definitions = ResourceManager.getDefinitions();
        addYamlKeyLookupElements(currentKey, definitions, parameters, result);
    }

    private void addYamlKeyLookupElements(@Nonnull final String key, @Nonnull final List<ResourceDefinition<?>> definitions,
                                          @Nonnull CompletionParameters parameters, @Nonnull final CompletionResultSet result) {
        final YAMLFile yamlFile = parameters.getOriginalFile() instanceof YAMLFile ? (YAMLFile) parameters.getOriginalFile() : null;
        if (Objects.isNull(yamlFile)) {
            return;
        }
        definitions.stream()
                .filter(d -> d instanceof SpringSupported<?>)
                .map(d -> (SpringSupported<?>) d)
                .flatMap(d -> d.getSpringProperties().stream().map(p -> Triple.of(p.getKey(), p.getValue(), d)))
                .filter(t -> !StringUtils.startsWith(t.getLeft(), "#")) // filter out commented properties
                .filter(t -> StringUtils.isBlank(key) || (StringUtils.startsWith(t.getLeft(), key) && !StringUtils.equals(t.getLeft(), key)))
                .filter(t -> YAMLUtil.getQualifiedKeyInFile(yamlFile, getKeyListForProperty(t.getLeft())) == null)
                .forEach(t -> result.addElement(createYamlKeyLookupElement(t.getLeft(), t.getRight())));
    }

    private LookupElement createYamlKeyLookupElement(@Nonnull final String property, @Nonnull final SpringSupported<?> definition) {
        return LookupElementBuilder.create(property)
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withBoldness(true)
                .withPresentableText(property)
                .withTypeText("String")
                .withInsertHandler((context, item) -> handleInsertYamlKey(property, context, item));
    }

    private void handleInsertYamlKey(@Nonnull final String property, InsertionContext context, LookupElement item) {
        final YAMLFile yamlFile = context.getFile() instanceof YAMLFile ? (YAMLFile) context.getFile() : null;
        if (Objects.isNull(yamlFile)) {
            return;
        }
        final Document document = context.getDocument();
        final int lineNumber = document.getLineNumber(context.getStartOffset());
        final int tailOffset = DocumentUtil.isLineEmpty(document, lineNumber) && context.getTailOffset() < document.getTextLength() ?
                context.getTailOffset() + 1 : context.getTailOffset(); // delete empty line
        document.deleteString(context.getStartOffset(), tailOffset);
        context.commitDocument();
        YamlUtils.insertYamlKeyValue(property, null, yamlFile, context);
        final YAMLKeyValue result = YAMLUtil.getQualifiedKeyInFile(yamlFile, getKeyListForProperty(property));
        context.getEditor().getCaretModel().moveToOffset(result.getTextRange().getEndOffset() + 1);
        AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
    }

    private String[] getKeyListForProperty(@Nonnull final String property) {
        final boolean isCommented = property.startsWith("#");
        final String unCommentedKeyString = isCommented ? StringUtils.trim(property.substring(1)) : property;
        final String[] keys = unCommentedKeyString.split("\\.");
        if (isCommented) {
            keys[keys.length - 1] = "# " + keys[keys.length - 1];
        }
        return keys;
    }
}
