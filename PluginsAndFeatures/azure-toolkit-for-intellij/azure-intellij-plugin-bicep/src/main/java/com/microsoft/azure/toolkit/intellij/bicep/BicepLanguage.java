/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep;

import com.intellij.lang.Language;

public class BicepLanguage extends Language {
    public static final BicepLanguage INSTANCE = new BicepLanguage();

    private BicepLanguage() {
        super("Bicep");
    }
}
