/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class ExceptionNode extends AbstractTreeNode<Throwable> {

    public ExceptionNode(@Nonnull Project project, final Throwable e) {
        super(project, e);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final ArrayList<AbstractTreeNode<?>> actionNodes = new ArrayList<>();
        final Throwable e = this.getValue();
        if (e instanceof AzureToolkitRuntimeException) {
            final Object[] actions = Optional.ofNullable(((AzureToolkitRuntimeException) e).getActions()).orElseGet(() -> new Object[0]);
            for (final Object action : actions) {
                if (action instanceof Action.Id) {
                    actionNodes.add(new ActionNode<>(this.getProject(), (Action.Id<Object>) action));
                } else if (action instanceof Action<?>) {
                    actionNodes.add(new ActionNode<>(this.getProject(), (Action<Object>) action));
                }
            }
        }
        return actionNodes;
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final String message = ExceptionUtils.getRootCauseMessage(this.getValue());
        presentation.addText(message, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
    }

    @Override
    public String toString() {
        return this.getValue().getMessage();
    }
}

