/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector;

import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Interfaced to indicate that the resource supports managed identity
 * <p>
 */
public interface IManagedIdentitySupported<T extends AzResource> {

    default Map<String, String> initIdentityEnv(AzureServiceResource<T> data, Project project){
        return Collections.emptyMap();
    }

    default List<String> getRequiredPermissions() {
        return Collections.emptyList();
    }

    @Nullable
    default Map<String, BuiltInRole> getBuiltInRoles() {
        return Collections.emptyMap();
    }
}
