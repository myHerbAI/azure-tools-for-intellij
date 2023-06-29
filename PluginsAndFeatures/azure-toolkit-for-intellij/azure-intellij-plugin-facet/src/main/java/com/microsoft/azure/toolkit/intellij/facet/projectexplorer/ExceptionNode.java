/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Slf4j
public class ExceptionNode extends AbstractTreeNode<Throwable> implements IAzureFacetNode {
    @Getter
    @Setter
    private boolean disposed;

    public ExceptionNode(@Nonnull IAzureFacetNode parent, final Throwable e) {
        super(parent.getProject(), e);
        if (!parent.isDisposed()) {
            Disposer.register(parent, this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final ArrayList<AbstractTreeNode<?>> children = new ArrayList<>();
        //noinspection UnstableApiUsage
        Disposer.disposeChildren(this, ignore -> true);
        if (this.isDisposed()) {
            return Collections.emptyList();
        }
        try {
            final Throwable e = this.getValue();
            if (e instanceof AzureToolkitRuntimeException) {
                final Object[] actions = Optional.ofNullable(((AzureToolkitRuntimeException) e).getActions()).orElseGet(() -> new Object[0]);
                for (final Object action : actions) {
                    if (action instanceof Action.Id) {
                        children.add(new ActionNode<>(this, (Action.Id<Object>) action));
                    } else if (action instanceof Action<?>) {
                        children.add(new ActionNode<>(this, (Action<Object>) action));
                    }
                }
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
        return children;
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

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        return null;
    }
}

