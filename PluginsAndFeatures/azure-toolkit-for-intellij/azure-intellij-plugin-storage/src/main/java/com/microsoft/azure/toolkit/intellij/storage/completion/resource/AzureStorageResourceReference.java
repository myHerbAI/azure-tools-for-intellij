/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AzureStorageResourceReference extends PsiReferenceBase<PsiElement> {
    public AzureStorageResourceReference(@NotNull PsiElement element, TextRange rangeInElement, boolean soft) {
        super(element, rangeInElement, soft);
    }

    public AzureStorageResourceReference(@NotNull PsiElement element, TextRange rangeInElement) {
        super(element, rangeInElement);
    }

    public AzureStorageResourceReference(@NotNull PsiElement element, boolean soft) {
        super(element, soft);
    }

    public AzureStorageResourceReference(@NotNull PsiElement element) {
        super(element);
    }

    @Override
    public @Nullable PsiElement resolve() {
        return new MyFakePsiElement();
    }

    class MyFakePsiElement extends FakePsiElement implements SyntheticElement {
        @Override
        public PsiElement getParent() {
            return myElement;
        }

        @Override
        public void navigate(boolean requestFocus) {
            System.out.println(getValue());
        }

        @Override
        public String getPresentableText() {
            return getValue();
        }


        @Override
        public String getName() {
            return getValue();
        }

        @Override
        public TextRange getTextRange() {
            final TextRange rangeInElement = getRangeInElement();
            final TextRange elementRange = myElement.getTextRange();
            return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
        }
    }
}
