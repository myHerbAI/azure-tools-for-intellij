/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.keyvaults;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class KeyVaultActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final String SERVICE_ACTIONS = "actions.keyvaults.service";
    public static final String KEY_VAULT_ACTIONS = "actions.keyvaults.instance";
    public static final String MODULE_ACTIONS = "actions.keyvaults.module";
    public static final String SECRET_ACTIONS = "actions.keyvaults.secret";
    public static final String SECRET_VERSION_ACTIONS = "actions.keyvaults.secret_version";
    public static final String CERTIFICATE_ACTIONS = "actions.keyvaults.certificate";
    public static final String CERTIFICATE_VERSION_ACTIONS = "actions.keyvaults.certificate_version";
    public static final String KEY_ACTIONS = "actions.keyvaults.key";
    public static final String KEY_VERSION_ACTIONS = "actions.keyvaults.key_version";
    public static final Action.Id<AzResource> ENABLE_CREDENTIAL = Action.Id.of("user/keyvaults.enable_credential.type|credential");
    public static final Action.Id<AzResource> DISABLE_CREDENTIAL = Action.Id.of("user/keyvaults.disable_credential.type|credential");
    public static final Action.Id<AzResource> DOWNLOAD_CREDENTIAL = Action.Id.of("user/keyvaults.download_credential.type|credential");
    public static final Action.Id<AzResource> SHOW_CREDENTIAL = Action.Id.of("user/keyvaults.show_credential.type|credential");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_KEY_VAULT = Action.Id.of("user/keyvaults.create_key_vault.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(ENABLE_CREDENTIAL)
                .withLabel("Enable")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .enableWhen(s -> s.getFormalStatus().isStopped())
                .register(am);

        new Action<>(DISABLE_CREDENTIAL)
                .withLabel("Disable")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .enableWhen(s -> s.getFormalStatus().isRunning())
                .register(am);

        new Action<>(DOWNLOAD_CREDENTIAL)
                .withLabel("Download")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .enableWhen(s -> s.getFormalStatus().isRunning())
                .register(am);

        new Action<>(SHOW_CREDENTIAL)
                .withLabel("Show Credential")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .enableWhen(s -> s.getFormalStatus().isRunning())
                .register(am);

        new Action<>(GROUP_CREATE_KEY_VAULT)
                .withLabel("Key vault")
                .withIdParam(AzResource::getName)
                .visibleWhen(s -> s instanceof ResourceGroup)
                .enableWhen(s -> s.getFormalStatus().isConnected())
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup redisActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                "---",
                ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(KEY_VAULT_ACTIONS, redisActionGroup);

        final ActionGroup keyVaultSubModuleActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(MODULE_ACTIONS, redisActionGroup);

        final ActionGroup credentialActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                "---",
                ResourceCommonActionsContributor.CREATE,
                ResourceCommonActionsContributor.DELETE,
                "---",
                SHOW_CREDENTIAL,
                DOWNLOAD_CREDENTIAL
        );
        am.registerGroup(SECRET_ACTIONS, redisActionGroup);
        am.registerGroup(CERTIFICATE_ACTIONS, redisActionGroup);
        am.registerGroup(KEY_ACTIONS, redisActionGroup);

        final ActionGroup credentialVersionActionGroup = new ActionGroup(
                ENABLE_CREDENTIAL,
                DISABLE_CREDENTIAL,
                SHOW_CREDENTIAL,
                DOWNLOAD_CREDENTIAL
        );
        am.registerGroup(SECRET_VERSION_ACTIONS, redisActionGroup);
        am.registerGroup(CERTIFICATE_VERSION_ACTIONS, redisActionGroup);
        am.registerGroup(KEY_VERSION_ACTIONS, redisActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_KEY_VAULT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
