/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.choice.ChoiceVariantIntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.Utils;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Annotator for validations of resource connection variables in String
 * Mostly it will check following items:
 * 1. whether connected resource is valid (deleted/broken)
 * 2. whether there are string follow the same pattern but did not appears in environment variables,
 * which means the correspond connection may be deleted
 */
public class ResourceConnectionAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (element instanceof PsiJavaToken && Objects.equals(((PsiJavaToken) element).getTokenType(), JavaTokenType.STRING_LITERAL)) {
            final PsiJavaToken token = (PsiJavaToken) element;
            final Module module = ModuleUtil.findModuleForPsiElement(element);
            final String value = StringUtils.strip(element.getText(), "\"");
            final Connection<? extends AzResource, ?> connection = Utils.getConnectionWithEnvironmentVariable(module, value);
            if (Objects.isNull(connection)) {
                // validate based on string pattern
                validateNamePattern(element, holder);
            } else {
                // validate resource
                validateConnectionResource(element, holder, connection);
            }
        }
    }

    private void validateNamePattern(@Nonnull final PsiElement element, @Nonnull final AnnotationHolder holder) {
        final Profile profile = Optional.ofNullable(ModuleUtil.findModuleForPsiElement(element))
                .map(AzureModule::from)
                .map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(profile)) {
            return;
        }
        final String text = StringUtils.strip(element.getText(), "\"");
        final AzureServiceResource.Definition<?> definition = ConnectionManager.getDefinitions().stream()
                .map(d -> d.getResourceDefinition() instanceof AzureServiceResource.Definition ?
                        (AzureServiceResource.Definition<?>) d.getResourceDefinition() : null)
                .filter(Objects::nonNull)
                .filter(d -> d.getEnvironmentVariablesKey().stream().anyMatch(k -> StringUtils.endsWith(text, StringUtils.removeStart(k, Connection.ENV_PREFIX))))
                .findFirst().orElse(null);
        if (Objects.nonNull(definition)) {
            final AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.WARNING, String.format("Could not find resource connection with variable %s", text))
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            final String suffix = definition.getEnvironmentVariablesKey().stream()
                    .map(key -> StringUtils.removeStart(key, Connection.ENV_PREFIX))
                    .filter(k -> StringUtils.endsWith(text, k))
                    .findFirst().orElse(null);
            final List<String> values = profile.getConnections().stream()
                    .filter(c -> Objects.equals(c.getResource().getDefinition(), definition))
                    .flatMap(c -> profile.getGeneratedEnvironmentVariables(c).stream())
                    .map(Pair::getKey)
                    .filter(key -> StringUtils.isNotBlank(suffix) && StringUtils.endsWith(key, suffix))
                    .toList();
            values.forEach(value -> createEnvironmentVariableQuickFix(value, element, builder));
            builder.create();
        }
    }

    private void createEnvironmentVariableQuickFix(@Nonnull final String value, @Nonnull final PsiElement element,
                                                   @Nonnull final AnnotationBuilder builder) {
        builder.withFix(new ChoiceVariantIntentionAction() {
            @Override
            public int getIndex() {
                return 0;
            }

            @Override
            public @IntentionName @NotNull String getName() {
                return String.format("Change to \"%s\"", value);
            }

            @Override
            public @IntentionFamilyName @NotNull String getFamilyName() {
                return "Resource Connection Fixes";
            }

            @Override
            public void applyFix(@NotNull Project project, PsiFile file, @Nullable Editor editor) {
                final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
                final PsiElement replacement = factory.createExpressionFromText(StringUtils.wrap(value, '"'), null).getFirstChild();
                element.replace(replacement);
            }
        });
    }

    private void validateConnectionResource(@Nonnull final PsiElement element, @Nonnull final AnnotationHolder holder,
                                            @Nonnull final Connection<? extends AzResource, ?> connection) {
        final AzResource data = connection.getResource().getData();
        if (!data.getFormalStatus().isConnected()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Connected resource is not available")
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .withFix(new EditConnectionFix(connection))
                    .create();
        }
    }

    @AllArgsConstructor
    private static class EditConnectionFix implements IntentionAction {
        private final Connection<? extends AzResource, ?> connection;

        @Override
        public @IntentionName @NotNull String getText() {
            return "Edit Connection";
        }

        @Override
        public @NotNull @IntentionFamilyName String getFamilyName() {
            return "Resource Connection Fixes";
        }

        @Override
        public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
            return true;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
            final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, "azure.annotator.action", context);
            AzureActionManager.getInstance().getAction(ResourceConnectionActionsContributor.EDIT_CONNECTION).handle(connection, event);
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
