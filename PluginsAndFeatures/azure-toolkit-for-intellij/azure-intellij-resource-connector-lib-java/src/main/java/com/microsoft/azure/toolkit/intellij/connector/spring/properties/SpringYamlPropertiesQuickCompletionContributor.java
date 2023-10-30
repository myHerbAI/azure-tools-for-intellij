package com.microsoft.azure.toolkit.intellij.connector.spring.properties;

import com.google.common.collect.ImmutableMap;
import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.completion.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import javax.annotation.Nonnull;
import java.util.List;

public class SpringYamlPropertiesQuickCompletionContributor extends CompletionContributor {

    public static final String APPLICATION = "application";

    public SpringYamlPropertiesQuickCompletionContributor() {
        super();
        extend(CompletionType.BASIC, SpringYamlPropertiesCompletionContributor.YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLKeyValue.class)), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@Nonnull final CompletionParameters parameters, @Nonnull final ProcessingContext context, @Nonnull final CompletionResultSet result) {
                final PsiElement element = parameters.getPosition();
                final String key = SpringYamlPropertiesCompletionProvider.getYamlElementKey(element);
                final List<? extends SpringSupported<?>> definitions = SpringPropertyValueCompletionProvider.getSupportedDefinitions(key);
                if (!definitions.isEmpty()) {
                    if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                        AzureTelemeter.info("connector.not_signed_in.yaml_value_code_completion", ImmutableMap.of("key", key));
                        result.addElement(LookupElements.buildSignInLookupElement());
                    } else {
                        result.addElement(LookupElements.buildConnectLookupElement(definitions.get(0), SpringPropertyValueCompletionProvider.PropertyValueInsertHandler::insert));
                    }
                }
            }
        });
    }
}
