/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.completion.properties;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.patterns.PlatformPatterns;

public class SpringPropertiesCompletionContributor extends CompletionContributor {
    public SpringPropertiesCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertiesTokenTypes.KEY_CHARACTERS), new SpringPropertyKeyCompletionProvider());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertiesTokenTypes.KEY_VALUE_SEPARATOR), new SpringPropertyValueCompletionProvider());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertiesTokenTypes.VALUE_CHARACTERS), new SpringPropertyValueCompletionProvider());
    }
}
