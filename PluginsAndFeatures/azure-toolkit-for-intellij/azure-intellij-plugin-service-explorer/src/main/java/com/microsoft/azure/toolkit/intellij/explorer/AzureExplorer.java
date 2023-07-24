/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.explorer;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.ide.common.genericresource.GenericResourceActionsContributor;
import com.microsoft.azure.toolkit.ide.common.genericresource.GenericResourceNode;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.Tree;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class AzureExplorer extends Tree {
    @Getter
    public static final AzureExplorerNodeProviderManager manager = new AzureExplorerNodeProviderManager();
    public static final String AZURE_ICON = AzureIcons.Common.AZURE.getIconPath();

    private AzureExplorer() {
        super();
        this.putClientProperty(PLACE, ResourceCommonActionsContributor.AZURE_EXPLORER);
        this.root = buildAzureRoot();
        this.init(this.root);
    }

    private static Node<Azure> buildAzureRoot() {
        final List<Node<?>> modules = getModules();
        return new Node<>(Azure.az())
            .withIcon(AZURE_ICON)
            .withLabel(getTitle())
            .withChildrenLoadLazily(false)
            .addChildren(modules);
    }

    public static Node<?> buildAppCentricViewRoot() {
        final AzureResources resources = Azure.az(AzureResources.class);
        return manager.createNode(resources, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
    }

    public static Node<?> buildFavoriteRoot() {
        return Favorites.buildFavoriteRoot(manager);
    }

    private static String getTitle() {
        try {
            final AzureAccount az = Azure.az(AzureAccount.class);
            final Account account = az.account();
            final List<Subscription> subscriptions = account.getSelectedSubscriptions();
            if (subscriptions.size() == 1) {
                return String.format("Azure(%s)", subscriptions.get(0).getName());
            }
        } catch (final Exception ignored) {
        }
        return "Azure";
    }

    @Nonnull
    public static List<Node<?>> getModules() {
        return manager.getRoots().stream()
            .map(r -> manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
            .collect(Collectors.toList());
    }

    public static void refreshAll() {
        AzureExplorer.manager.getRoots().stream().filter(r -> r instanceof AbstractAzResourceModule)
            .forEach(r -> ((AbstractAzResourceModule<?, ?, ?>) r).refresh());
        Favorites.getInstance().refresh();
    }

    public static class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory {
        public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
            final SimpleToolWindowPanel windowPanel = new SimpleToolWindowPanel(true, true);
            windowPanel.setContent(new AzureExplorer());
            final ContentFactory contentFactory = ContentFactory.getInstance();
            final Content content = contentFactory.createContent(windowPanel, null, false);
            toolWindow.getContentManager().addContent(content);
        }
    }

    public static class AzureExplorerNodeProviderManager implements IExplorerNodeProvider.Manager {
        private static final ExtensionPointName<IExplorerNodeProvider> providers =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.explorerNodeProvider");

        @Nonnull
        public List<Object> getRoots() {
            return providers.getExtensionList().stream()
                .map(IExplorerNodeProvider::getRoot)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        @Nonnull
        @Override
        public Node<?> createNode(@Nonnull Object o, Node<?> parent, IExplorerNodeProvider.ViewType type) {
            return providers.getExtensionList().stream()
                .filter(p -> p.accept(o, parent, type)).findAny()
                .map(p -> p.createNode(o, parent, this))
                .or(() -> Optional.of(o).filter(r -> r instanceof AbstractAzResource).map(AzureExplorerNodeProviderManager::createGenericNode))
                .orElseThrow(() -> new AzureToolkitRuntimeException(String.format("failed to render %s", o.toString())));
        }

        private static <U> U createGenericNode(Object o) {
            //noinspection unchecked
            return (U) new GenericResourceNode((AbstractAzResource<?, ?, ?>) o)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                .withActions(GenericResourceActionsContributor.GENERIC_RESOURCE_ACTIONS);
        }
    }
}

