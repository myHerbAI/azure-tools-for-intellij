package com.microsoft.azure.toolkit.intellij.connector.spring.properties;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

public class SpringYamlCompletionContributor extends CompletionContributor {

    public static final ElementPattern<? extends PsiFile> APPLICATION_YAML_FILES = PlatformPatterns.psiFile(YAMLFile.class).withName(StandardPatterns.string().startsWith("application"));
    public static final PsiJavaElementPattern.Capture<PsiElement> YAML_TEXT = PsiJavaPatterns.psiElement(YAMLTokenTypes.TEXT).inFile(APPLICATION_YAML_FILES);

    public SpringYamlCompletionContributor() {
        super();
        final SpringYamlKeyCompletionProvider keyCompletionProvider = new SpringYamlKeyCompletionProvider();
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLDocument.class)), keyCompletionProvider);
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLMapping.class)), keyCompletionProvider);
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLKeyValue.class)), keyCompletionProvider);
        final SpringYamlValueCompletionProvider valueCompletionProvider = new SpringYamlValueCompletionProvider();
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLKeyValue.class)), valueCompletionProvider);
    }
}
