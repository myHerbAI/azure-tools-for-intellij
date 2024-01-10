/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry;

import com.intellij.docker.registry.DockerRegistryConfiguration;
import com.intellij.docker.registry.DockerRegistryManager;
import com.intellij.docker.view.registry.node.DockerRegistryRoot;
import com.intellij.execution.services.ServiceEventListener;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.containerregistry.servicesview.AzureContainerRegistryProvider;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class IntelliJContainerRegistryActionsContributorForDockerPlugin implements IActionsContributor {
    public static final String NULL_SERVICE_MESSAGE = "Failed to get Docker Registries root in \"Services\" view, please check whether \"Docker\" plugin is correctly installed.";

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.OPEN_IN_SERVICES_VIEW, (r, e) -> r instanceof ContainerRegistry,
            (AbstractAzResource<?, ?, ?> r, AnActionEvent e) -> AzureTaskManager.getInstance()
                .runLater(() -> openRegistryInServicesView((ContainerRegistry) r, Objects.requireNonNull(e.getProject())))
        );
    }

    private static void openRegistryInServicesView(@Nonnull final ContainerRegistry data, @Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Services");
        if (Objects.nonNull(toolWindow) && !toolWindow.isActive()) {
            toolWindow.activate(() -> openRegistryInServicesView(data, project));
            return;
        }
        final ServiceViewContributor<?> root = Arrays.stream(ServiceViewContributor.CONTRIBUTOR_EP_NAME.getExtensions())
            .filter(serviceView -> StringUtils.equalsIgnoreCase(serviceView.getViewDescriptor(project).getId(), "Docker Registries"))
            .findFirst().orElse(null);
        if (Objects.isNull(root)) {
            AzureMessager.getMessager().warning(NULL_SERVICE_MESSAGE);
            return;
        }
        final ServiceViewContributor<?> registry = getRegistryService(data, project, root).orElse(null);
        if (Objects.nonNull(registry)) {
            focusInServicesView(registry, root, project);
        } else {
            addAndFocusInServicesView(data, root, project);
        }
    }

    private static void addAndFocusInServicesView(@Nonnull final ContainerRegistry data, ServiceViewContributor<?> root, @Nonnull final Project project) {
        final DockerRegistryManager registryManager = DockerRegistryManager.getInstance();
        final Predicate<DockerRegistryConfiguration> exists = r -> r.getAddress().equals(data.getLoginServerUrl()) && r.getUsername().equalsIgnoreCase(data.getUserName());
        if (registryManager.getRegistries().stream().noneMatch(exists)) {
            final DockerRegistryConfiguration config = new DockerRegistryConfiguration().withName(data.getName());
            config.setRegistryProviderId(AzureContainerRegistryProvider.ID);
            config.setAddress(data.getLoginServerUrl());
            config.setUsername(data.getUserName());
            config.setPasswordSafe(data.getPrimaryCredential());

            final MessageBus messageBus = project.getMessageBus();
            final MessageBusConnection connect = messageBus.connect();
            connect.subscribe(ServiceEventListener.TOPIC, (ServiceEventListener) serviceEvent -> {
                final boolean isDockerRegistryEvent = serviceEvent.contributorClass == root.getClass();
                if (isDockerRegistryEvent) {
                    getRegistryService(data, project, root).ifPresent(service -> {
                        focusInServicesView(service, root, project);
                        connect.disconnect();
                    });
                }
            });

            registryManager.addRegistry(config);
        }
    }

    private static void focusInServicesView(@Nonnull final ServiceViewContributor<?> service,
                                            @Nonnull final ServiceViewContributor<?> contributor, final Project project) {
        ServiceViewManager.getInstance(project).select(service, contributor.getClass(), true, true);
        ServiceViewManager.getInstance(project).expand(service, contributor.getClass());
    }

    private static Optional<DockerRegistryRoot> getRegistryService(final @Nonnull ContainerRegistry data, final @Nonnull Project project, final ServiceViewContributor<?> root) {
        return root.getServices(project).stream()
            .filter(c -> c instanceof DockerRegistryRoot)
            .map(c -> (DockerRegistryRoot) c)
            .filter(r -> StringUtils.equals(r.getConfiguration().getAddress(), data.getLoginServerUrl()) && StringUtils.equals(r.getConfiguration().getUsername(), data.getUserName()))
            .findFirst();
    }

}
