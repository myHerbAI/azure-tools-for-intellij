/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.completion.provider;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.DocumentUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class SpringYAMLCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final Key<YAMLFile> YAML_FILE_KEY = Key.create("com.microsoft.azure.SpringYAMLCompletionProvider.YAML_FILE");

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final String currentKey = getYamlElementKey(element);
        final List<ResourceDefinition<?>> definitions = ResourceManager.getDefinitions();
        addYamlKeyLookupElements(currentKey, definitions, parameters, result);
        addYamlValueLookupElements(currentKey, definitions, parameters, result);
    }

    private void addYamlValueLookupElements(@Nonnull final String key, @Nonnull final List<ResourceDefinition<?>> definitions,
                                            @Nonnull CompletionParameters parameters, @Nonnull final CompletionResultSet result) {
        // find correspond resource definition
        final SpringSupported<?> springSupported = definitions.stream()
                .filter(d -> d instanceof SpringSupported<?>)
                .map(d -> (SpringSupported<?>) d)
                .filter(d -> d.getSpringProperties().stream().anyMatch(p -> StringUtils.equals(p.getKey(), key)))
                .findFirst().orElse(null);
        final Module module = ModuleUtil.findModuleForPsiElement(parameters.getOriginalFile());
        if (Objects.isNull(module) || Objects.isNull(springSupported)) {
            return;
        }
        final List<? extends Resource<?>> resources = springSupported.getResources(module.getProject());
        final Pair<String, String> property = springSupported.getSpringProperties().stream()
                .filter(p -> StringUtils.equals(p.getKey(), key))
                .findFirst().orElseThrow(() -> new RuntimeException("cannot find property with key " + key));
        resources.stream()
                .map(resource -> createYamlValueLookupElement(module, resource, springSupported, property.getValue()))
                .filter(Objects::nonNull)
                .forEach(result::addElement);
    }

    @Nullable
    private LookupElement createYamlValueLookupElement(@Nonnull final Module module, @Nonnull final Resource<?> azResource,
                                                       @Nonnull final SpringSupported<?> definition, @Nonnull final String template) {
        final AzureModule azureModule = AzureModule.from(module);
        final Profile defaultProfile = azureModule.getDefaultProfile();
        final List<Connection<?, ?>> connections = Objects.isNull(defaultProfile) ? Collections.emptyList() : defaultProfile.getConnections();
        final Connection<?, ?> connection = connections.stream()
                .filter(c -> Objects.equals(c.getResource(), azResource)).findFirst()
                .orElseGet(() -> createConnection(module, azResource));
        final String elementValue = getPropertiesCompletionValue(template, connection);
        return LookupElementBuilder.create(elementValue)
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withBoldness(true)
                .withPresentableText(azResource.getName())
                .withTypeText("String")
                .withLookupStrings(List.of(elementValue, azResource.getName()))
                .withInsertHandler((context, item) -> {
                    saveConnection(azureModule, connection);
                    handleInsertYamlConnection(connection, context, item);
                });
    }

    private void handleInsertYamlConnection(@Nonnull final Connection<?, ?> connection, InsertionContext context, LookupElement item) {
        final YAMLFile yamlFile = context.getFile() instanceof YAMLFile ? (YAMLFile) context.getFile() : null;
        if (Objects.isNull(yamlFile)) {
            return;
        }
        final ResourceDefinition<?> definition = connection.getResource().getDefinition();
        final List<Pair<String, String>> springProperties = ((SpringSupported<?>) definition).getSpringProperties();
        for (final Pair<String, String> pair : springProperties) {
            final YAMLKeyValue target = getYamlKeyValueElement(yamlFile, pair.getKey());
            if (Objects.isNull(target) || StringUtils.isBlank(target.getValueText())) {
                final String key = pair.getKey();
                final String value = getPropertiesCompletionValue(pair.getValue(), connection);
                insertYamlKeyValue(key, value, yamlFile, context, item);
            }
        }
    }

    private String getPropertiesCompletionValue(@Nonnull final String template, @Nonnull final Connection<?, ?> connection) {
        return StringUtils.replace(template, Connection.ENV_PREFIX, connection.getEnvPrefix());
    }

    private YAMLKeyValue getYamlKeyValueElement(@Nonnull final YAMLFile yamlFile, @Nonnull final String property) {
        return YAMLUtil.getQualifiedKeyInFile(yamlFile, getKeyListForProperty(property));
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

    private static Connection<?, ?> createConnection(@Nonnull Module module, @Nonnull Resource<?> resource) {
        final Resource<?> consumer = ModuleResource.Definition.IJ_MODULE.define(module.getName());
        // todo: set connection env prefix
        final ConnectionDefinition connectionDefinition =
                ConnectionManager.getDefinitionOrDefault(resource.getDefinition(), consumer.getDefinition());
        final Connection<?, ?> result = connectionDefinition.define(resource, consumer);
        result.setEnvPrefix(StringUtils.upperCase(resource.getName()));
        return result;
    }

    private static void saveConnection(@Nonnull final AzureModule module, @Nonnull final Connection<?, ?> connection) {
        final AzureTaskManager taskManager = AzureTaskManager.getInstance();
        taskManager.runOnPooledThread(() ->
                taskManager.runLater(() ->
                        taskManager.write(() -> {
                            final Profile profile = Optional.ofNullable(module.getDefaultProfile())
                                    .orElseGet(module::initializeWithDefaultProfileIfNot);
                            profile.createOrUpdateConnection(connection);
                            profile.save();
                        })));
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
                .filter(t -> getYamlKeyValueElement(yamlFile, t.getLeft()) == null)
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
        insertYamlKeyValue(property, null, yamlFile, context, item);
        final YAMLKeyValue result = getYamlKeyValueElement(yamlFile, property);
        context.getEditor().getCaretModel().moveToOffset(result.getTextRange().getEndOffset() + 1);
        AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
    }

    private void insertYamlKeyValue(@Nonnull final String property, @Nullable String value, @Nonnull final YAMLFile yamlFile,
                                    InsertionContext context, LookupElement item) {
        final Document document = context.getDocument();
        final Pair<YAMLKeyValue, List<String>> latestKeyValue = getLatestKeyValue(yamlFile, property);
        final YAMLKeyValue parent = latestKeyValue.getKey();
        final YAMLValue parentValue = Optional.ofNullable(parent).map(YAMLKeyValue::getValue).orElse(null);
        final List<String> keyList = latestKeyValue.getValue();
        if (Objects.isNull(parent)) {
            // there is no parent, so we can insert the key at the beginning of the document
            final StringBuilder result = new StringBuilder(YAMLElementGenerator.createChainedKey(keyList, 0));
            result.append(StringUtils.SPACE);
            if (StringUtils.isNotBlank(value)) {
                result.append(value);
            }
            // add lf if first line is not empty
            final String firstLine = document.getText(new TextRange(0, document.getLineEndOffset(0)));
            if (StringUtils.isNotBlank(firstLine)) {
                result.append(StringUtils.LF);
            }
            document.insertString(0, result.toString());
            context.commitDocument();
            return;
        }
        final int indent = Optional.ofNullable(parentValue)
                .map(YAMLUtil::getIndentToThisElement)
                .orElse(YAMLUtil.getIndentToThisElement(parent) + 2);
        final StringBuilder insertContent = new StringBuilder(YAMLElementGenerator.createChainedKey(keyList, indent));
        insertContent.append(StringUtils.SPACE);
        if (StringUtils.isNotBlank(value)) {
            insertContent.append(value);
        }
        final int index = Optional.ofNullable(parentValue)
                .map(YAMLValue::getTextOffset)
                .orElseGet(() -> parent.getTextRange().getEndOffset());
        if (document.getLineNumber(parent.getTextOffset()) == document.getLineNumber(index)) {
            // if insert in the same line as parent
            insertContent.insert(0, StringUtils.repeat(' ', indent));
            insertContent.insert(0, StringUtils.LF);
        } else {
            insertContent.append(StringUtils.LF);
            // need add indent for brother element
            if (parentValue instanceof YAMLMapping) {
                insertContent.append(StringUtils.repeat(' ', indent));
            }
        }
        final String content = insertContent.toString();
        document.insertString(index, content);
        context.commitDocument();
    }

    @Nonnull
    private Pair<YAMLKeyValue, List<String>> getLatestKeyValue(@Nonnull final YAMLFile file, final String properties) {
        final String[] keys = getKeyListForProperty(properties);
        for (int i = keys.length - 1; i > 0; i--) {
            final YAMLKeyValue result = YAMLUtil.getQualifiedKeyInFile(file, ArrayUtils.subarray(keys, 0, i));
            if (Objects.nonNull(result)) {
                return Pair.of(result, Arrays.stream(ArrayUtils.subarray(keys, i, keys.length)).toList());
            }
        }
        return Pair.of(null, Arrays.stream(keys).toList());
    }

    private String getYamlElementKey(@Nonnull final PsiElement position) {
        final StringBuilder result = new StringBuilder();
        PsiElement element = position;
        while (Objects.nonNull(element)) {
            if (element instanceof YAMLKeyValue) {
                if (result.length() != 0) {
                    result.insert(0, ".");
                }
                result.insert(0, ((YAMLKeyValue) element).getKeyText());
            }
            element = element.getParent();
        }
        return result.toString();
    }

}
