/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ActionNode<T> extends AbstractTreeNode<Action<T>> implements IAzureFacetNode {
    @Nullable
    private final T source;
    @Getter
    @Setter
    private boolean disposed;

    protected ActionNode(@Nonnull IAzureFacetNode parent, Action<T> action) {
        super(parent.getProject(), action);
        this.source = null;
        if (!parent.isDisposed()) {
            Disposer.register(parent, this);
        }
    }

    protected ActionNode(@Nonnull IAzureFacetNode parent, Action.Id<T> actionId) {
        super(parent.getProject(), IntellijAzureActionManager.getInstance().getAction(actionId));
        this.source = null;
        Disposer.register(parent, this);
    }

    protected ActionNode(@Nonnull IAzureFacetNode parent, Action<T> action, @Nullable T source) {
        super(parent.getProject(), action);
        this.source = source;
        Disposer.register(parent, this);
    }

    protected ActionNode(@Nonnull IAzureFacetNode parent, Action.Id<T> actionId, @Nullable T source) {
        super(parent.getProject(), IntellijAzureActionManager.getInstance().getAction(actionId));
        this.source = source;
        Disposer.register(parent, this);
    }

    @Override
    public @Nonnull Collection<? extends AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void update(@Nonnull PresentationData presentation) {
        final IView.Label view = this.getValue().getView(this.source);
        presentation.addText(StringUtils.capitalize(view.getLabel()), SimpleTextAttributes.LINK_ATTRIBUTES);
        presentation.setTooltip(view.getDescription());
    }

    @Override
    public void onClicked(Object event) {
        this.getValue().handle(this.source, event);
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return new IActionGroup() {
            @Override
            public IView.Label getView() {
                return null;
            }

            @Override
            public List<Object> getActions() {
                return Collections.singletonList(ActionNode.this.getValue());
            }

            @Override
            public void addAction(Object action) {
                // do nothing here
            }
        };
    }

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, Action.SOURCE) ? this.source : null;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public String toString() {
        return this.getValue().toString();
    }
}
