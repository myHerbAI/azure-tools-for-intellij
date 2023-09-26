/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.Sortable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class SortableTreeNode extends DefaultMutableTreeNode implements Sortable {

    private Node node;

    public SortableTreeNode() {
        super();
    }

    public SortableTreeNode(Node userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
        this.node = userObject;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(MutableTreeNode newChild) {
        super.add(newChild);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void insert(MutableTreeNode newChild, int childIndex) {
        super.insert(newChild, childIndex);
    }

    @Override
    public int getPriority() {
        return node == null ? DEFAULT_PRIORITY : node.getPriority();
    }
}
