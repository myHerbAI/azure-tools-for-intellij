/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.LeafState;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GenericResourceNode extends AbstractTreeNode<ResourceId> implements IAzureFacetNode {
    private final String status;
    @Getter
    @Setter
    private boolean disposed;

    protected GenericResourceNode(Project project, @Nonnull ResourceId id, String status) {
        super(project, id);
        this.status = status;
    }

    @Override
    public @Nonnull Collection<? extends AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void update(@Nonnull PresentationData presentation) {
        presentation.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Resources.GENERIC_RESOURCE));
        presentation.addText(this.getValue().name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.addText(StringUtils.SPACE + this.status, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return getValue();
        }
        return null;
    }

    @Override
    public @Nonnull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }
}
