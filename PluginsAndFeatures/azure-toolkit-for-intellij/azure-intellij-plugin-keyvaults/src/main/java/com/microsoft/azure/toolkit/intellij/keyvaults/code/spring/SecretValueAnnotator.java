/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults.code.spring;

import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class SecretValueAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (EnvVarCompletionContributor.PROPERTY_VALUE.accepts(element)) {
            final String key = element.getParent().getFirstChild().getText();
            final String value = element.getText();
            if (EnvVarCompletionContributor.isSecretKey(key) && !EnvVarCompletionContributor.isSecreted(value)) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Secret is in plain text.")
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.WARNING)
                    .withFix(new IntentionAndQuickFixAction() {
                        @Override
                        public @IntentionName @NotNull String getName() {
                            //noinspection DialogTitleCapitalization
                            return "Save in Azure KeyVaults";
                        }

                        @Override
                        public @IntentionFamilyName @NotNull String getFamilyName() {
                            return "Azure general fixes";
                        }

                        @Override
                        public void applyFix(@NotNull final Project project, final PsiFile file, @Nullable final Editor editor) {

                        }
                    })
                    .create();
            }
        }
    }
}
