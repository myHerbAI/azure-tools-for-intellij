/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.execution.services.ServiceEventListener;
import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.execution.services.ServiceViewLazyContributor;
import com.intellij.execution.services.ServiceViewProvidingContributor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class AzureResourceViewContributor implements ServiceViewProvidingContributor<AzureResourceViewContributor, Node<?>>, ServiceViewLazyContributor, Node.ViewRenderer, Node.ChildrenRenderer {
    private final Node<?> node;

    public AzureResourceViewContributor(@Nonnull Node<?> node) {
        this.node = node;
        this.node.setViewRenderer(this);
        this.node.setChildrenRenderer(this);
    }

    @Override
    public @NotNull ServiceViewDescriptor getViewDescriptor(@NotNull final Project project) {
        return new AzureResourceViewDescriptor(project, this.node);
    }

    @Override
    public @NotNull List<AzureResourceViewContributor> getServices(@NotNull final Project project) {
        return this.node.getChildren().stream().map(AzureResourceViewContributor::new).collect(Collectors.toList());
    }

    @Override
    public @NotNull ServiceViewDescriptor getServiceDescriptor(@NotNull final Project project, @NotNull final AzureResourceViewContributor contributor) {
        return contributor.getViewDescriptor(project);
    }

    @Override
    public @NotNull Node<?> asService() {
        return this.node;
    }

    @Override
    public void updateView() {
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        final ServiceEventListener.EventType type = ServiceEventListener.EventType.SERVICE_CHANGED;
        final ServiceEventListener.ServiceEvent event = ServiceEventListener.ServiceEvent.createEvent(type, this, AzureServiceViewContributor.class);
        bus.syncPublisher(ServiceEventListener.TOPIC).handle(event);
    }

    @Override
    public void updateChildren(final boolean... booleans) {
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        final ServiceEventListener.EventType type = ServiceEventListener.EventType.SERVICE_CHILDREN_CHANGED;
        final ServiceEventListener.ServiceEvent event = ServiceEventListener.ServiceEvent.createEvent(type, this, AzureServiceViewContributor.class);
        bus.syncPublisher(ServiceEventListener.TOPIC).handle(event);
    }
}
