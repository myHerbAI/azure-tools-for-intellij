/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.properties;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager.AzureResourceFileType;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;

import javax.annotation.Nonnull;
import javax.swing.*;

public class IntellijShowPropertiesViewAction {
    private static final AzureResourceEditorViewManager manager = new AzureResourceEditorViewManager((resource) -> {
        final Icon icon = getFileTypeIcon(resource);
        final String name = getFileTypeName(resource);
        return new AzureResourceFileType(name, icon);
    });

    public static void showPropertyView(@Nonnull AzResourceBase resource, @Nonnull Project project) {
        manager.showEditor(resource, project);
    }

    public static void closePropertiesView(@Nonnull AzResourceBase resource, @Nonnull Project project) {
        manager.closeEditor(resource, project);
    }

    private static String getFileTypeName(@Nonnull AzResourceBase resource) {
        if (resource instanceof AzResource) {
            return getNewFileTypeName((AzResource) resource);
        }
        return String.format("%s_FILE_TYPE", resource.getClass().getSimpleName().toUpperCase());
    }

    private static Icon getFileTypeIcon(@Nonnull AzResourceBase resource) {
        if (resource instanceof AzResource) {
            return IntelliJAzureIcons.getIcon(getNewFileTypeIcon((AzResource) resource));
        }
        return IntelliJAzureIcons.getIcon(String.format("/icons/%s.svg", resource.getClass().getSimpleName().toLowerCase()));
    }

    @Nonnull
    private static String getNewFileTypeIcon(AzResource resource) {
        return String.format("/icons/%s/default.svg", resource.getFullResourceType());
    }

    @Nonnull
    private static String getNewFileTypeName(AzResource resource) {
        return resource.getFullResourceType().replaceAll("/", ".");
    }
}
