/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.*;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.code.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

// todo: this should belong to connector common library
public class FunctionConnectionCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final Map<String, String> ANNOTATION_DEFINITION_MAP = new HashMap<>() {{
        put("com.microsoft.azure.functions.annotation.BlobInput", "Storage");
        put("com.microsoft.azure.functions.annotation.BlobOutput", "Storage");
        put("com.microsoft.azure.functions.annotation.BlobTrigger", "Storage");
        put("com.microsoft.azure.functions.annotation.QueueOutput", "Storage");
        put("com.microsoft.azure.functions.annotation.QueueTrigger", "Storage");
        put("com.microsoft.azure.functions.annotation.TableInput", "Storage");
        put("com.microsoft.azure.functions.annotation.TableOutput", "Storage");
        put("com.microsoft.azure.functions.annotation.TableTrigger", "Storage");
        put("com.microsoft.azure.functions.annotation.StorageAccount", "Storage");
        put("com.microsoft.azure.functions.annotation.CosmosDBInput", "DocumentDB");
        put("com.microsoft.azure.functions.annotation.CosmosDBOutput", "DocumentDB");
        put("com.microsoft.azure.functions.annotation.CosmosDBTrigger", "DocumentDB");
    }};
    public static final PsiJavaElementPattern.Capture<PsiElement> STORAGE_ACCOUNT_ANNOTATION = psiElement()
            .insideAnnotationParam("com.microsoft.azure.functions.annotation.StorageAccount");
    public static final ElementPattern CONNECTION_NAME_VALUE = PsiJavaPatterns.psiNameValuePair().withName("connection").withParent(
            PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
                    PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("functionAnnotation") {
                        @Override
                        public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                            return ANNOTATION_DEFINITION_MAP.containsKey(psiAnnotation.getQualifiedName());
                        }
                    })));
    public static final ElementPattern CONNECTION_ANNOTATION = psiElement().withSuperParent(2, CONNECTION_NAME_VALUE);
    public static final ElementPattern FUNCTION_ANNOTATION_CONNECTION_PATTERN = PlatformPatterns.or(STORAGE_ACCOUNT_ANNOTATION, CONNECTION_ANNOTATION);

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        final FunctionSupported<?> resourceDefinition = Optional.ofNullable(annotation)
                .map(FunctionConnectionCompletionProvider::getResourceDefinition).orElse(null);
        if (Objects.isNull(resourceDefinition)) {
            return;
        }
        final Module module = ModuleUtil.findModuleForPsiElement(parameters.getPosition());
        final AzureModule azureModule = Optional.ofNullable(module).map(AzureModule::from).orElse(null);
        // get all connection string from function definition
        addExistingConnectionLookupElements(azureModule, result, resourceDefinition);
        // add create connection string lookup element
        addCreateNewConnectionLookupElement(parameters, result, resourceDefinition);
    }

    public static FunctionSupported<?> getResourceDefinition(@Nonnull final PsiAnnotation annotation) {
        final String qualifiedName = annotation.getQualifiedName();
        final String resourceType = ANNOTATION_DEFINITION_MAP.get(qualifiedName);
        return ResourceManager.getDefinitions().stream()
                .filter(definition -> definition instanceof FunctionSupported<?> &&
                        StringUtils.equalsIgnoreCase(((FunctionSupported<?>) definition).getResourceType(), resourceType))
                .map(d -> (FunctionSupported<?>) d)
                .findFirst().orElse(null);
    }

    private void addExistingConnectionLookupElements(AzureModule azureModule, CompletionResultSet result, FunctionSupported<?> resourceDefinition) {
        final List<Connection<?, ?>> connections = Optional.ofNullable(azureModule)
                .map(AzureModule::getDefaultProfile)
                .map(Profile::getConnections).orElse(Collections.emptyList());
        connections.stream()
                .filter(connection -> Objects.equals(connection.getResource().getDefinition(), resourceDefinition))
                .map(this::createExistingLookupElement)
                .forEach(result::addElement);
    }

    private LookupElement createExistingLookupElement(Connection<?, ?> connection) {
        final Resource<?> resource = connection.getResource();
        final FunctionSupported<?> definition = (FunctionSupported<?>) resource.getDefinition();
        return LookupElementBuilder.create(connection.getEnvPrefix())
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withBoldness(true)
                .withTypeText(definition.getTitle())
                .withInsertHandler((context, item) -> onInsertConnection(connection, context))
                .withTailText(String.format(" (%s : %s)", resource.getName(), definition.getTitle()));
    }

    private void addCreateNewConnectionLookupElement(CompletionParameters parameters, CompletionResultSet result, FunctionSupported<?> resourceDefinition) {
        final LookupElement lookupElement = LookupElements.buildConnectLookupElement(resourceDefinition, this::onInsertConnection);
        result.addElement(lookupElement);
    }

    private void onInsertConnection(@Nullable final Connection<?, ?> connection, @Nonnull final InsertionContext context) {
        if (Objects.isNull(connection)) {
            return;
        }
        final PsiElement element = PsiUtil.getElementAtOffset(context.getFile(), context.getStartOffset());
        final boolean hasSpace = element.getPrevSibling() instanceof PsiWhiteSpace;
        final FunctionSupported<?> definition = (FunctionSupported<?>) connection.getResource().getDefinition();
        final String property = definition.getFunctionProperty(connection);
        final String insertValue = psiElement().inside(literalExpression()).accepts(element) ? property : String.format("%s\"%s\"", hasSpace ? "" : " ", property);
        context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), insertValue);
        context.commitDocument();
    }
}
