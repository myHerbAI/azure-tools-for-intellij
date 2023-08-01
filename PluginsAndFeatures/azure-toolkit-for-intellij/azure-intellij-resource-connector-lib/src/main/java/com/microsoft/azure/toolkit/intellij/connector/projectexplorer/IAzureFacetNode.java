/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

public interface IAzureFacetNode extends DataProvider, Disposable {
    @Nullable
    default IActionGroup getActionGroup() {
        return null;
    }

    default void onClicked(AnActionEvent event) {

    }

    default void onDoubleClicked(AnActionEvent event) {

    }

    Project getProject();

    void updateChildren();

    void updateView();

    default void dispose() {
        setDisposed(true);
    }

    boolean isDisposed();

    void setDisposed(boolean isDisposed);


    public static AbstractAzureFacetNode<?> toExceptionNode(Throwable e, @Nonnull Project project) { // `static` to make it available for AzureFacetRootNode
        e = ExceptionUtils.getRootCause(e);
        if (e instanceof AzureToolkitAuthenticationException) {
            final Action<Object> signin = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE).bind(project).withLabel("Sign in to manage connected resource");
            return new ActionNode<>(project, signin);
        } else {
            return new ExceptionNode(project, e);
        }
    }

    public static Collection<? extends AbstractAzureFacetNode<?>> handleException(Callable<Collection<? extends AbstractAzureFacetNode<?>>> t, Project project) { // `static` to make it available for AzureFacetRootNode
        try {
            return t.call();
        } catch (final Throwable e) {
            final ArrayList<AbstractAzureFacetNode<?>> children = new ArrayList<>();
            children.add(toExceptionNode(e, project));
            return children;
        }
    }
}
