/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.intellij.execution.services.ServiceEventListener;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.kubernetes.api.Context;
import com.intellij.kubernetes.api.KubernetesApiProvider;
import com.intellij.kubernetes.api.settings.KubernetesApiProjectSettings;
import com.intellij.kubernetes.view.KubernetesServiceViewContributor;
import com.intellij.kubernetes.view.KubernetesServiceViewContributorServicesHolder;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class OpenKubernetesPluginAction {
    public static void selectKubernetesInKubernetesPlugin(@Nonnull final KubernetesCluster cluster, @Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Services");
        if (!toolWindow.isActive()) {
            toolWindow.activate(() -> selectKubernetesInKubernetesPlugin(cluster, project));
            return;
        }
        // check whether exists correspond kubernetes context (by nane)
        final KubernetesApiProvider apiProvider = KubernetesApiProvider.Companion.getInstance(project);
        final KubernetesApiProjectSettings projectSettings = KubernetesApiProjectSettings.Companion.getInstance(project);
        final Context context = apiProvider.getExistingContexts().stream()
                .filter(c -> StringUtils.equals(c.getName(), cluster.name()))
                .findFirst().orElseGet(() -> getOrAddClusterContext(cluster, project));
        final ServiceViewContributor<?> service = getServiceView(cluster, project);
        if (Objects.isNull(service)) {
            // todo: add error handling if service is not correctlly added
            return;
        }
        ServiceViewManager.getInstance(project).select(service, KubernetesServiceViewContributor.class, true, true);
        ServiceViewManager.getInstance(project).expand(service, KubernetesServiceViewContributor.class);
    }

    // todo: get better method to bind service view and cluster
    private static ServiceViewContributor<?> getServiceView(@Nonnull final KubernetesCluster cluster, @Nonnull final Project project) {
        ServiceEventListener serviceEventListener = null;
        return KubernetesServiceViewContributorServicesHolder.Companion.getInstance(project).getServices().stream()
                .filter(s -> StringUtils.equals(getServiceViewName(s, project), cluster.name()))
                .findFirst().orElse(null);
    }

    private static String getServiceViewName(@Nonnull final ServiceViewContributor<?> view, @Nonnull final Project project) {
        final ItemPresentation presentation = view.getViewDescriptor(project).getPresentation();
        return Optional.ofNullable(presentation.getPresentableText())
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> presentation instanceof PresentationData ?
                        ((PresentationData) presentation).getColoredText().get(0).getText() : presentation.toString());
    }

    public static Context getOrAddClusterContext(@Nonnull final KubernetesCluster cluster, @Nonnull final Project project) {
        if (Objects.isNull(getAvailableContext(cluster, project))) {
            GetKubuCredentialAction.getKubuCredential(cluster, project, false);
            KubernetesApiProvider.Companion.getInstance(project).refreshConfiguration$intellij_clouds_kubernetes();
        }
        final Context result = getAvailableContext(cluster, project);
        if (Objects.isNull(result)) {
            return null;
        }
        final KubernetesApiProjectSettings projectSettings = KubernetesApiProjectSettings.Companion.getInstance(project);
        final KubernetesApiProvider apiProvider = KubernetesApiProvider.Companion.getInstance(project);
        apiProvider.addContext$intellij_clouds_kubernetes(result);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // todo: investigate whether to wait for context to be added
        return result;
    }

    public static Context getAvailableContext(@Nonnull final KubernetesCluster cluster, @Nonnull final Project project) {
        final KubernetesApiProvider apiProvider = KubernetesApiProvider.Companion.getInstance(project);
        return apiProvider.getActualState$intellij_clouds_kubernetes().getAvailableContexts().stream()
                .filter(c -> StringUtils.equals(c.getName(), cluster.name()))
                .findFirst().orElse(null);
    }

}
