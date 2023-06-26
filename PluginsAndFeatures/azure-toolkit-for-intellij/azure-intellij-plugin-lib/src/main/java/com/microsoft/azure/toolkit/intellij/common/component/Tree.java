/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.TreeUIHelper;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

@Getter
public class Tree extends SimpleTree implements DataProvider {
    protected Node<?> root;

    public Tree() {
        super();
    }

    public Tree(Node<?> root) {
        this(root, null);
    }

    public Tree(Node<?> root, @Nullable String place) {
        super();
        this.root = root;
        this.putClientProperty(Action.PLACE, place);
        init(root);
    }

    protected void init(@Nonnull Node<?> root) {
        ComponentUtil.putClientProperty(this, ANIMATION_IN_RENDERER_ALLOWED, true);
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUtil.installActions(this);
        TreeUIHelper.getInstance().installTreeSpeedSearch(this);
        TreeUIHelper.getInstance().installSmartExpander(this);
        TreeUIHelper.getInstance().installSelectionSaver(this);
        TreeUIHelper.getInstance().installEditSourceOnEnterKeyHandler(this);
        this.setCellRenderer(new NodeRenderer());
        this.setModel(new DefaultTreeModel(new TreeNode<>(root, this)));
        TreeUtils.installExpandListener(this);
        TreeUtils.installSelectionListener(this);
        TreeUtils.installMouseListener(this);
    }

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        if (StringUtils.equals(dataId, Action.SOURCE)) {
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) this.getLastSelectedPathComponent();
            if (Objects.nonNull(selectedNode)) {
                return selectedNode.getUserObject();
            }
        }
        return null;
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    public static class TreeNode<T> extends DefaultMutableTreeNode implements Node.ViewRenderer, Node.ChildrenRenderer {
        @Nonnull
        @EqualsAndHashCode.Include
        protected final Node<T> inner;
        protected final JTree tree;
        Boolean loaded = null; //null:not loading/loaded, false: loading: true: loaded

        private final Debouncer updateChildrenLater = new TailingDebouncer(this::doUpdateChildren, 300);

        public TreeNode(@Nonnull Node<T> n, JTree tree) {
            super(n.getValue(), n.hasChildren());
            this.inner = n;
            this.tree = tree;
            if (this.getAllowsChildren()) {
                this.add(new LoadingNode());
            }
            if (!this.inner.isLazy()) {
                this.loadChildren();
            }
            this.inner.setViewRenderer(this);
            this.inner.setChildrenRenderer(this);
        }

        @Nullable
        public String getPlace() {
            return TreeUtils.getPlace(this.tree);
        }

        @Override
        @EqualsAndHashCode.Include
        // NOTE: equivalent nodes in same tree will cause rendering problems.
        public javax.swing.tree.TreeNode getParent() {
            return super.getParent();
        }

        public String getLabel() {
            return this.inner.getLabel();
        }

        public List<IView.Label> getInlineActionViews() {
            return this.inner.getInlineActions().stream()
                .map(action -> action.getView(this.inner.getValue(), this.getPlace()))
                .filter(IView.Label::isEnabled)
                .collect(Collectors.toList());
        }

        private void doUpdateChildren() {
            final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
            if (Objects.nonNull(model) && (Objects.nonNull(this.getParent()) || Objects.equals(model.getRoot(), this))) {
                AzureTaskManager.getInstance().runLater(() -> {
                    synchronized (this.tree) {
                        if (Objects.nonNull(this.getParent()) || Objects.equals(model.getRoot(), this)) {
                            try {
                                model.nodeStructureChanged(this);
                            } catch (final NullPointerException ignored) {
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void updateView() {
            final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
            if (Objects.nonNull(model) && (Objects.nonNull(this.getParent()) || Objects.equals(model.getRoot(), this))) {
                AzureTaskManager.getInstance().runLater(() -> {
                    synchronized (this.tree) {
                        if (Objects.nonNull(this.getParent()) || Objects.equals(model.getRoot(), this)) {
                            try {
                                model.nodeChanged(this);
                            } catch (final NullPointerException ignored) {
                            }
                        }
                    }
                });
            }
        }

        @Override
        @AzureOperation(name = "user/common.load_children.node", params = "this.getLabel()")
        public synchronized void updateChildren(boolean... incremental) {
            AzureTaskManager.getInstance().runLater(() -> { // queue up/wait until pending UI update finishes.
                if (this.getAllowsChildren() && BooleanUtils.isNotFalse(this.loaded)) {
                    final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
                    if (incremental.length > 0 && incremental[0] && Objects.nonNull(model)) {
                        this.removeLoadMoreNode();
                    } else {
                        this.removeAllChildren();
                    }
                    this.add(new LoadingNode());
                    this.updateChildrenLater.debounce();
                    this.loaded = null;
                    this.loadChildren(incremental);
                }
            });
        }

        protected synchronized void loadChildren(boolean... incremental) {
            if (loaded != null) {
                return; // return if loading/loaded
            }
            this.loaded = false;
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            tm.runOnPooledThread(() -> {
                final List<Node<?>> children = this.inner.getChildren();
                if (incremental.length > 0 && incremental[0]) {
                    tm.runLater(() -> updateChildren(children));
                } else {
                    tm.runLater(() -> setChildren(children));
                }
            });
        }

        private synchronized void setChildren(List<Node<?>> children) {
            this.removeAllChildren();
            children.stream().map(c -> new TreeNode<>(c, this.tree)).forEach(this::add);
            this.addLoadMoreNode();
            this.loaded = true;
            this.updateChildrenLater.debounce();
        }

        private synchronized void updateChildren(List<Node<?>> children) {
            final Map<Node<?>, TreeNode<?>> oldChildren = IntStream.range(0, this.getChildCount() - 1).mapToObj(this::getChildAt)
                .filter(n -> n instanceof TreeNode<?>).map(n -> ((TreeNode<?>) n))
                .collect(Collectors.toMap(n -> n.inner, n -> n));

            final Set<Node<?>> newChildrenNodes = new HashSet<>(children);
            final Set<Node<?>> oldChildrenNodes = oldChildren.keySet();
            Sets.difference(oldChildrenNodes, newChildrenNodes).forEach(o -> oldChildren.get(o).removeFromParent());

            for (int i = 0; i < children.size(); i++) {
                final Node<?> node = children.get(i);
                if (!oldChildrenNodes.contains(node)) {
                    final TreeNode<?> treeNode = new TreeNode<>(node, this.tree);
                    this.insert(treeNode, i);
                } else { // discarded nodes should be disposed manually to unregister listeners.
                    node.dispose();
                }
            }

            this.removeLoadingNode();
            this.addLoadMoreNode();
            this.updateChildrenLater.debounce();
            this.loaded = true;
        }

        public synchronized void clearChildren() {
            synchronized (this.tree) {
                this.removeAllChildren();
                this.loaded = null;
                if (this.getAllowsChildren()) {
                    this.add(new LoadingNode());
                    AzureTaskManager.getInstance().runLater(() -> this.tree.collapsePath(new TreePath(this.getPath())));
                }
                this.updateChildrenLater.debounce();
            }
        }

        @Override
        public void setParent(MutableTreeNode newParent) {
            super.setParent(newParent);
            if (this.getParent() == null) {
                this.inner.dispose();
            }
        }

        private void removeLoadingNode() {
            this.children().asIterator().forEachRemaining(c -> {
                if (c instanceof LoadingNode) {
                    ((LoadingNode) c).removeFromParent();
                }
            });
        }

        private void addLoadMoreNode() {
            if (this.inner.hasMoreChildren()) {
                this.add(new LoadMoreNode());
            }
        }

        private void removeLoadMoreNode() {
            this.children().asIterator().forEachRemaining(c -> {
                if (c instanceof LoadMoreNode) {
                    ((LoadMoreNode) c).removeFromParent();
                }
            });
        }
    }

    public static class NodeRenderer extends com.intellij.ide.util.treeView.NodeRenderer {

        @Override
        public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof TreeNode) {
                TreeUtils.renderMyTreeNode(tree, (TreeNode<?>) value, selected, this);
            } else {
                super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        }
    }

    public static class LoadMoreNode extends DefaultMutableTreeNode {
        public static final String LABEL = "load more...";

        public LoadMoreNode() {
            super(LABEL);
        }

        public void load() {
            Optional.ofNullable(this.getParent()).map(p -> (TreeNode<?>) p).map(p -> p.inner).ifPresent(Node::loadMoreChildren);
        }
    }
}
