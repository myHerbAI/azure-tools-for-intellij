/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.explorer;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.genericresource.GenericResourceActionsContributor;
import com.microsoft.azure.toolkit.ide.common.genericresource.GenericResourceNode;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;

public class AzureExplorer {
    private static final AzureExplorerNodeProviderManager manager = new AzureExplorerNodeProviderManager();

    public static Node<?>[] getModules() {
        return manager.getRoots().stream()
                .map(r -> manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
                .toArray(Node<?>[]::new);
    }

    private static class AzureExplorerNodeProviderManager implements IExplorerNodeProvider.Manager {
        private static final String EXTENSION_POINT_ID = "com.microsoft.azure.toolkit.eclipse.explorer";

        public List<Object> getRoots() {
            IConfigurationElement[] configurationElements = Platform.getExtensionRegistry()
                    .getConfigurationElementsFor(EXTENSION_POINT_ID);
            return Arrays.stream(configurationElements).map(element -> {
                try {
                    return element.createExecutableExtension("implementation");
                } catch (CoreException e) {
                    return null;
                }
            }).filter(object -> object instanceof IExplorerNodeProvider).map(p -> (IExplorerNodeProvider) p)
                    .map(IExplorerNodeProvider::getRoot).filter(Objects::nonNull).collect(Collectors.toList());
        }

        @Override
        public Node<?> createNode(@Nonnull Object o, Node<?> parent, IExplorerNodeProvider.ViewType type) {
            IConfigurationElement[] configurationElements = Platform.getExtensionRegistry()
                    .getConfigurationElementsFor(EXTENSION_POINT_ID);
            return Arrays.stream(configurationElements).map(element -> {
                try {
                    return element.createExecutableExtension("implementation");
                } catch (CoreException e) {
                    return null;
                }
            }).filter(object -> object instanceof IExplorerNodeProvider).map(object -> (IExplorerNodeProvider) object)
                    .filter(p -> p.accept(o, parent, type)).findAny().map(p -> p.createNode(o, parent, this))
                    .orElseGet(() -> Optional.of(o).filter(r -> r instanceof AbstractAzResource)
                            .map(m -> ((Node) AzureExplorerNodeProviderManager.createGenericNode(m)))
                            .orElseThrow(() -> new AzureToolkitRuntimeException(
                                    String.format("failed to render %s", o.toString()))));
        }

        private static <U> U createGenericNode(Object o) {
            //noinspection unchecked
            return (U) new GenericResourceNode((AbstractAzResource<?, ?, ?>) o)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                .withActions(GenericResourceActionsContributor.GENERIC_RESOURCE_ACTIONS);
        }
    }
}
