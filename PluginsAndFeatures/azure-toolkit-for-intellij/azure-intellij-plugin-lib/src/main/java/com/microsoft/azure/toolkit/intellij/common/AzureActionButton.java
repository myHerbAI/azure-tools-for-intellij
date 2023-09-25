/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.Optional;

public class AzureActionButton<T> extends JButton {
    public static final Key<ActionEvent> ACTION_EVENT_KEY = new Key<>("AzureActionButton.actionEvent");
    private final ActionListener listener = this::onActionPerformed;
    protected Action<T> action;

    public AzureActionButton() {
        super();
    }

    public AzureActionButton(@Nonnull final Action.Id<T> actionId) {
        this(AzureActionManager.getInstance().getAction(actionId));
    }

    public AzureActionButton(@Nonnull final Action<T> action) {
        super();
        this.action = action;
        this.registerActionListener();
    }

    public void setAction(@Nonnull final Action.Id<T> actionId) {
        setAction(actionId, null);
    }

    public void setAction(@Nonnull final Action.Id<T> actionId, @Nullable T source) {
        final Action<T> action = AzureActionManager.getInstance().getAction(actionId);
        this.setAction(Objects.isNull(source) ? action : action.bind(source));
    }

    public void setAction(@Nonnull final Action<T> action) {
        this.action = action;
        this.registerActionListener();
    }

    private void registerActionListener() {
        final boolean registered = ArrayUtils.contains(this.getActionListeners(), this.listener);
        if (!registered) {
            this.addActionListener(this.listener);
        }
    }

    private void onActionPerformed(ActionEvent actionEvent) {
        final DataContext context = DataManager.getInstance().getDataContext(this);
        if (context instanceof UserDataHolder) {
            ((UserDataHolder) context).putUserData(ACTION_EVENT_KEY, actionEvent);
        }
        // todo: use panel name as the action place
        final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, "actionButton", context);
        Optional.ofNullable(action).ifPresent(a -> a.handle(null, event));
    }
}
