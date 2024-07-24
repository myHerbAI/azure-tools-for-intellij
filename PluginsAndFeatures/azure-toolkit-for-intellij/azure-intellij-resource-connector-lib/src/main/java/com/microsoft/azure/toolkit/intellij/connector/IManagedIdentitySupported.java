/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector;

import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Interfaced to indicate that the resource supports managed identity
 * <p>
 */
public interface IManagedIdentitySupported<T extends AzResource> {

    // check whether identity is connected to resource with full permission

    Map<String, String> initIdentityEnv(Connection<T, ?> data, Project project);

    List<String> getRequiredPermissions();

    @Nullable Map<String, BuiltInRole> getBuiltInRoles();

    public static boolean checkPermission(@Nonnull AzureServiceResource<?> data, @Nonnull String identity) {
        final AzureServiceResource.Definition<?> d = data.getDefinition();
        final AzResource r = data.getData();
        if (d instanceof IManagedIdentitySupported<?> definition && r instanceof AbstractAzResource<?, ?, ?> resource) {
            final List<String> permissions = resource.getPermissions(identity);
            final List<String> requiredPermissions = definition.getRequiredPermissions();
            return CollectionUtils.containsAll(permissions, requiredPermissions);
        }
        return true;
    }

    @Nonnull
    public static Action<?> getOpenIdentityConfigurationAction(@Nonnull AzureServiceResource<?> data) {
        final String url = Objects.requireNonNull(data.getData()).getPortalUrl() + "/iamAccessControl";
        return AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).bind(url).withLabel("Open IAM Configuration");
    }

    @Nullable
    public static Action<?> getGrantPermissionAction(@Nonnull AzureServiceResource<?> data, @Nonnull String identity) {
        final AzureServiceResource.Definition<?> d = data.getDefinition();
        final AzResource r = data.getData();
        if (d instanceof IManagedIdentitySupported<?> definition && r instanceof AbstractAzResource<?, ?, ?> resource) {
            return new Action<>(Action.Id.of("user/common.assign_role.identity"))
                    .withLabel("Assign Required Roles")
                    .withIdParam(identity)
                    .withHandler(ignore -> {
                        final Map<String, BuiltInRole> builtInRoles = definition.getBuiltInRoles();
                        final List<String> roles = resource.getRoleAssignments(identity).stream().map(RoleAssignment::roleDefinitionId).toList();
                        try {
                            Objects.requireNonNull(definition.getBuiltInRoles()).forEach((id, role) -> {
                                if (roles.stream().noneMatch(ro -> ro.endsWith(id))) {
                                    AzureMessager.getMessager().info(String.format("Assign role %s to identity %s...", role, identity));
                                    resource.grantPermissionToIdentity(identity, role);
                                }
                            });
                        } catch (final RuntimeException e) {
                            AzureMessager.getMessager().warning(String.format("Failed to grant permission to identity %s, please try assign correct role to it in portal", identity), e);
                        }
                    });
        }
        return null;
    }
}
