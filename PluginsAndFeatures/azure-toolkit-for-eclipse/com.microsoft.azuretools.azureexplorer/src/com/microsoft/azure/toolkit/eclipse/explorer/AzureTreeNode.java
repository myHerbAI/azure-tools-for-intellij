/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.explorer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;

import com.microsoft.azure.toolkit.eclipse.common.EclipseAzureIcons;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node.ChildrenRenderer;
import com.microsoft.azure.toolkit.ide.common.component.Node.ViewRenderer;
import com.microsoft.azure.toolkit.lib.common.action.Action.View;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionInstance;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import com.microsoft.azure.toolkit.lib.common.view.IView.Label;

public class AzureTreeNode implements ViewRenderer, ChildrenRenderer {
	private TreeViewer treeView;
	private AzureTreeNode parent;
	private List<AzureTreeNode> childs;

	private Boolean loaded = null; // null:not loading/loaded, false: loading: true: loaded
	private com.microsoft.azure.toolkit.ide.common.component.Node<?> node;

	public AzureTreeNode(TreeViewer treeView, AzureTreeNode parent,
			com.microsoft.azure.toolkit.ide.common.component.Node<?> node) {
		super();
		this.treeView = treeView;
		this.node = node;
		this.parent = parent;
		if (!node.isLazy()) {
			loadChildren();
		}
		node.setViewRenderer(this);
	}

	public List<AzureTreeNode> getChildren() {
		if (loaded == null) {
			loadChildren();
		}
		return BooleanUtils.isTrue(loaded) ? childs : Collections.emptyList();
	}

	private void loadChildren() {
		if (loaded != null) {
			return; // return if loading/loaded
		}
		this.loaded = Boolean.FALSE;
		AzureTaskManager.getInstance().runOnPooledThread(() -> {
			try {
				childs = AzureTreeNode.this.node.getChildren().stream()
						.map(node -> new AzureTreeNode(treeView, this, node)).collect(Collectors.toList());
			} catch (Exception e) {
				AzureTaskManager.getInstance().runLater(() -> AzureMessager.getMessager().warning(e.getMessage()));
				e.printStackTrace();
				childs = Collections.emptyList();
			} finally {
				loaded = true;
				AzureTaskManager.getInstance().runLater(() -> updateView());
			}
		});
	}

	public boolean hasChildren() {
		return BooleanUtils.isTrue(loaded) && node.hasChildren();
	}

	public AzureTreeNode getParent() {
		return this.parent;
	}

	public void onNodeClick() {
		if (loaded == null) {
			loadChildren();
			updateView();
		}
	}

	@Override
	public void updateChildren(boolean... incremental) {
		this.loaded = null;
		this.loadChildren();
		treeView.refresh(this);
	}

	@Override
	public void updateView() {
		treeView.refresh(this);
	}

	public String getText() {
		final String label = node.getLabel();
		return BooleanUtils.isFalse(loaded) ? label + " (Refreshing...)" : label;
	}

	public AzureIcon getAzureIcon() {
		return node.buildIcon();
	}

	public void installActionsMenu(@Nonnull IMenuManager manager) {
		Optional.ofNullable(node.getActions())
				.ifPresent(group -> applyActionGroupToMenu(group, manager, node.getValue()));
	}

	public void clearNode() {
		Optional.ofNullable(childs).ifPresent(childs -> childs.forEach(AzureTreeNode::clearNode));
		this.childs = Collections.emptyList();
		treeView.refresh(this);
		this.loaded = null;
	}

	private void applyActionGroupToMenu(@Nonnull IActionGroup actionGroup, @Nonnull IMenuManager manager,
			@Nullable Object source) {
		final AzureActionManager actionManager = AzureActionManager.getInstance();
		for (Object raw : actionGroup.getActions()) {
			Action action = null;
			if (raw instanceof com.microsoft.azure.toolkit.lib.common.action.Action.Id) {
				action = toEclipseAction(
						actionManager.getAction((com.microsoft.azure.toolkit.lib.common.action.Action.Id) raw), source);
			}
			if (raw instanceof String) {
				final String actionId = (String) raw;
				if (actionId.startsWith("-")) {
					final String title = actionId.replaceAll("-", "").trim();
					if (StringUtils.isBlank(title)) {
						manager.add(new Separator());
					} else {
						manager.add(new Separator(title));
					}
				} else if (StringUtils.isNotBlank(actionId)) {
					action = toEclipseAction(actionManager
							.getAction(com.microsoft.azure.toolkit.lib.common.action.Action.Id.of(actionId)), source);
				}
			} else if (raw instanceof com.microsoft.azure.toolkit.lib.common.action.Action) {
				action = toEclipseAction((com.microsoft.azure.toolkit.lib.common.action.Action) raw, source);
			} else if (raw instanceof ActionGroup) {
				applyActionGroupToMenu((ActionGroup) raw, manager, source);
			}
			if (action != null && !ResourceCommonActionsContributor.PIN.getId().equals(action.getId())) {
				manager.add(action);
			}
		}
	}

	private <T> Action toEclipseAction(com.microsoft.azure.toolkit.lib.common.action.Action<T> action,
			@Nullable T source) {
		final Label view = Optional.ofNullable(action).map(act -> act.getView(source)).orElse(null);
		if (view == null || StringUtils.isBlank(view.getLabel())) {
			return null;
		}
		final ImageDescriptor imageDescriptor = StringUtils.isEmpty(view.getIconPath()) ? null
				: EclipseAzureIcons.getIcon(view.getIconPath());
		final Action eclipseAction = new Action(view.getLabel(), imageDescriptor) {
			@Override
			public void run() {
				action.handle(source);
			}

			@Override
			public String getId() {
				return action.getId().getId();
			}
		};
		final ActionInstance<T> instance = action.instantiate(source);
		if (Objects.nonNull(instance)) {
			eclipseAction.setEnabled(instance.getView().isEnabled());
		} else {
			eclipseAction.setEnabled(false);
		}
		return eclipseAction;
	}
}
