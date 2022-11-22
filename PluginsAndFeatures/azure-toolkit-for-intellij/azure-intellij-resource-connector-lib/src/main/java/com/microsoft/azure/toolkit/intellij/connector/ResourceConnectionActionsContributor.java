/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;

public class ResourceConnectionActionsContributor implements IActionsContributor {
    public static final Action.Id<Object> REFRESH_CONNECTIONS = Action.Id.of("connector.refresh_connections");
    public static final Action.Id<Module> ADD_CONNECTION = Action.Id.of("connector.add_connection");
    public static final Action.Id<Connection<?, ?>> EDIT_CONNECTION = Action.Id.of("connector.edit_connection");
    public static final Action.Id<Connection<?, ?>> REMOVE_CONNECTION = Action.Id.of("connector.remove_connection");
    public static final String MODULE_ACTIONS = "actions.connector.module";
    public static final String CONNECTION_ACTIONS = "actions.connector.connection";

    @Override
    public void registerActions(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> refreshHandler = (project, e) -> Objects.requireNonNull(e.getProject())
                .getMessageBus().syncPublisher(ConnectionTopics.CONNECTIONS_REFRESHED)
                .connectionsRefreshed();
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", AzureIcons.Action.REFRESH.getIconPath())
                .title(t -> OperationBundle.description("connector.refresh_connections"));
        final Action<Object> refreshAction = new Action<>(REFRESH_CONNECTIONS, refreshHandler, refreshView);

        final Consumer<Module> addHandler = (m) -> openDialog(null, new ModuleResource(m.getName()), m.getProject());
        final ActionView.Builder addView = new ActionView.Builder("Add", AzureIcons.Action.ADD.getIconPath())
                .title(t -> OperationBundle.description("connector.add_connection"))
                .enabled(m -> m instanceof Module);
        final Action<Module> addAction = new Action<>(ADD_CONNECTION, addHandler, addView);

        final BiConsumer<Connection<?, ?>, AnActionEvent> editHandler = (c, e) -> openDialog(c, e.getProject());
        final ActionView.Builder editView = new ActionView.Builder("Edit", AzureIcons.Action.EDIT.getIconPath())
                .title(t -> OperationBundle.description("connector.edit_connection"))
                .enabled(m -> m instanceof Connection);
        final Action<Connection<?, ?>> editAction = new Action<>(EDIT_CONNECTION, editHandler, editView);

        final BiConsumer<Connection<?, ?>, AnActionEvent> removeHandler =
                (c, e) -> {
                    final Project project = Objects.requireNonNull(e.getProject());
                    project.getService(ConnectionManager.class).removeConnection(c.getResource().getId(), c.getConsumer().getId());
                    project.getMessageBus().syncPublisher(CONNECTION_CHANGED).connectionChanged(project, c, ConnectionTopics.Action.REMOVE);
                };
        final ActionView.Builder removeView = new ActionView.Builder("Remove", AzureIcons.Action.REMOVE.getIconPath())
                .title(t -> OperationBundle.description("connector.remove_connection"))
                .enabled(m -> m instanceof Connection);
        final Action<Connection<?, ?>> removeAction = new Action<>(REMOVE_CONNECTION, removeHandler, removeView);

        am.registerAction(refreshAction);
        am.registerAction(addAction);
        am.registerAction(editAction);
        am.registerAction(removeAction);
        IActionsContributor.super.registerActions(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup moduleActions = new ActionGroup(
                ADD_CONNECTION
        );
        am.registerGroup(MODULE_ACTIONS, moduleActions);

        final ActionGroup connectionActions = new ActionGroup("",
                EDIT_CONNECTION,
                REMOVE_CONNECTION
        );
        am.registerGroup(CONNECTION_ACTIONS, connectionActions);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    }

    private void openDialog(@Nullable Resource<?> r, @Nullable Resource<?> c, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setConsumer(c);
            dialog.setResource(r);
            dialog.show();
        });
    }

    private void openDialog(Connection<?, ?> c, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setValue(c);
            dialog.show();
        });
    }
}
