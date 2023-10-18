package com.microsoft.azure.toolkit.intellij.connector.spring.properties;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

public class SpringYamlPropertiesCompletionContributor extends CompletionContributor {

    public static final String APPLICATION = "application";

    public SpringYamlPropertiesCompletionContributor() {
        super();
        final PsiJavaElementPattern.Capture<PsiElement> textPattern = PsiJavaPatterns.psiElement(YAMLTokenTypes.TEXT)
                        .with(new PatternCondition<>("spring-yaml-file") {
                            @Override
                            public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
                                final PsiFile file = element.getContainingFile();
                                return file instanceof YAMLFile && StringUtils.startsWith(file.getName(), APPLICATION);
                            }
                        });
        extend(CompletionType.BASIC, textPattern.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLKeyValue.class)), new SpringYamlPropertiesCompletionProvider());
        extend(CompletionType.BASIC, textPattern.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLMapping.class)), new SpringYamlPropertiesCompletionProvider());
        extend(CompletionType.BASIC, textPattern.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLDocument.class)), new SpringYamlPropertiesCompletionProvider());
    }
}
