/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.action;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.Action.Id;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;


public class EclipseAzureActionManager extends AzureActionManager {
    private static final String ACTIONS_CATEGORY = "com.microsoft.azure.toolkit.eclipse.actions.category";
    private static final String EXTENSION_POINT_ID = "com.microsoft.azure.toolkit.eclipse.actions";
    private static final String ACTION_ID_PREFIX = "com.microsoft.azure.toolkit.eclipse.actions.";
    private static final Map<String, ActionGroup> groups = new HashMap<>();
    private static final Map<String, Action<?>> actions = new HashMap<>();
    private static final ICommandService cmdService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
    private static final IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);

    public static void register() {
        final EclipseAzureActionManager am = new EclipseAzureActionManager();
        register(am);
        IConfigurationElement[] configurationElements = Platform.getExtensionRegistry()
                .getConfigurationElementsFor(EXTENSION_POINT_ID);
        Arrays.stream(configurationElements).map(element -> {
            try {
                return element.createExecutableExtension("implementation");
            } catch (CoreException e1) {
                // swallow exception during register
                return null;
            }
        }).filter(ext -> ext instanceof IActionsContributor).map(ext -> (IActionsContributor) ext)
                .sorted(Comparator.comparing(IActionsContributor::getOrder))
                .forEachOrdered(contributor -> {
                    contributor.registerActions(am);
                    contributor.registerHandlers(am);
                    contributor.registerGroups(am);
                });
    }

    @Override
    public <D> Action<D> getAction(Id<D> id) {
        final String actionId = ACTION_ID_PREFIX + id.getId();
        Command command = cmdService.getCommand(actionId);
        if (!command.isDefined()) {
            return null;
        }
        return Optional.ofNullable((Action<D>) actions.get(actionId)).orElseGet(() -> {
            return new Action<>(id, (D d, ExecutionEvent event) -> {
                try {
                    handlerService.executeCommand(actionId, null);
                } catch (org.eclipse.core.commands.ExecutionException | NotDefinedException | NotEnabledException | NotHandledException error) {
                    error.printStackTrace();
                }
            }).setAuthRequired(false);
        });
    }

    @Override
    public ActionGroup getGroup(String id) {
        return groups.get(id);
    }

    @Override
    public <D> void registerAction(Action<D> action) {
        final String actionId = ACTION_ID_PREFIX + action.getId();
        Command command = cmdService.getCommand(actionId);
        if (command.isDefined()) {
            command.undefine();
        }
        command.define(actionId, actionId, getActionCategory());
        handlerService.activateHandler(actionId, new AbstractHandler() {
            @Override
            public Object execute(ExecutionEvent event) throws org.eclipse.core.commands.ExecutionException {
                action.handle((D) event.getApplicationContext(), event);
                return null;
            }

        });
        actions.put(actionId, action);
    }

    @Override
    public void registerGroup(String id, ActionGroup group) {
        groups.put(id, group);
    }

    public Category getActionCategory() {
        final Category category = cmdService.getCategory(ACTIONS_CATEGORY);
        if (!category.isDefined()) {
            category.define("AzureToolkitForEclipse", "Actions for Azure Toolkit for Eclipse");
        }
        return category;
    }
}
