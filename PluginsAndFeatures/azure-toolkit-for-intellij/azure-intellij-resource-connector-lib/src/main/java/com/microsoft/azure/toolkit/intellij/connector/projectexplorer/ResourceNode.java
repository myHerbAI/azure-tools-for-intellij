/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.LeafState;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ResourceNode extends AbstractAzureFacetNode<Node<?>> implements Node.ChildrenRenderer, Node.ViewRenderer {
    private final AbstractAzureFacetNode<?> parent;

    public ResourceNode(@Nonnull Project project, final Node<?> node, AbstractAzureFacetNode<?> parent) {
        super(project, node);
        this.parent = parent;
        node.setViewRenderer(this);
        node.setChildrenRenderer(this);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
        final Node<?> node = this.getValue();
        final ArrayList<AbstractAzureFacetNode<?>> children = new ArrayList<>(node.getChildren().stream().map(n -> new ResourceNode(this.getProject(), n, this)).toList());
        if (node.hasMoreChildren()) {
            final Action<Object> loadMoreAction = new Action<>(Action.Id.of("user/common.load_more"))
                .withHandler(i -> node.loadMoreChildren())
                .withLabel("load more")
                .withAuthRequired(true);
            children.add(new ActionNode<>(this.getProject(), loadMoreAction));
        }
        return children;
    }

    @Override
    public void buildView(final PresentationData presentation) {
        final Node<?> node = this.getValue();
        final Node.View view = node.getView();
        presentation.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
        presentation.addText(view.getLabel(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.setTooltip(view.getTips());
        Optional.ofNullable(view.getDescription()).ifPresent(d -> presentation.addText(" " + d, SimpleTextAttributes.GRAYED_ATTRIBUTES));
    }

    @Override
    public void updateChildren(boolean... incremental) {
        updateChildren();
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return Optional.ofNullable(getValue()).map(Node::getValue).orElse(null);
        }
        return null;
    }

    @Override
    public void onDoubleClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.doubleClick(event));
    }

    @Override
    public void onClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.click(event));
    }

    @Override
    @Nullable
    public IActionGroup getActionGroup() {
        IActionGroup group = Optional.ofNullable(getValue()).map(Node::getActions).orElse(null);
        final Object value = this.getValue().getValue();
        if (this.parent instanceof DeploymentTargetsNode targets && value instanceof AbstractAzResource<?, ?, ?> resource) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            final Action<Object> removeTarget = new Action<>(Action.Id.of("user/connector.remove_target.app"))
                .withLabel("Remove")
                .withIdParam(this.getValue().getLabel())
                .withIcon(AzureIcons.Action.DELETE.getIconPath())
                .withHandler(ignore -> tm.runLater(() -> tm.write(() -> targets.getValue().getProfile().removeApp(resource).save())))
                .withAuthRequired(false);
            if (group != null) {
                group = new ActionGroup();
                group.appendActions(removeTarget, "---");
                group.appendActions(group.getActions());
            } else {
                group = new ActionGroup(removeTarget);
            }
        }
        return group;
    }

    @Override
    public void dispose() {
        super.dispose();
        Optional.ofNullable(getValue()).ifPresent(Node::dispose);
    }

    @EqualsAndHashCode.Include
    public AbstractTreeNode<?> getMyParent() {
        return this.parent;
    }

    @Override
    public @Nonnull LeafState getLeafState() {
        return LeafState.ASYNC;
    }
}