/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.properties;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager.AzureResourceFileType;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.swing.*;

public class IntellijShowPropertiesViewAction {
    private static final AzureResourceEditorViewManager manager = new AzureResourceEditorViewManager((resource) -> {
        final Icon icon = getFileTypeIcon(resource);
        final String name = getFileTypeName(resource);
        return new AzureResourceFileType(name, icon);
    });

    public static void showPropertyView(@Nonnull AzResource resource, @Nonnull Project project) {
        manager.showEditor(resource, project);
    }

    public static void closePropertiesView(@Nonnull AzResource resource, @Nonnull Project project) {
        manager.closeEditor(resource, project);
    }

    private static String getFileTypeName(@Nonnull AzResource resource) {
        return resource.getFullResourceType().replaceAll("/", ".");
    }

    private static Icon getFileTypeIcon(@Nonnull AzResource resource) {
        final String path = String.format("/icons/%s/default.svg", resource.getFullResourceType());
        return IntelliJAzureIcons.getIcon(path);
    }
}
