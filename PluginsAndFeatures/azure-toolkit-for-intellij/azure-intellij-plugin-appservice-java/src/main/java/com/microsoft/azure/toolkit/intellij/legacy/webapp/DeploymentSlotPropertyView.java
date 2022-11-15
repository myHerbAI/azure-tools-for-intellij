/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotPropertyViewPresenter;

import javax.annotation.Nonnull;

public class DeploymentSlotPropertyView extends WebAppBasePropertyView {
    private static final String ID = "com.microsoft.intellij.helpers.webapp.DeploymentSlotPropertyView";

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppBasePropertyView create(@Nonnull final Project project, @Nonnull final String sid,
                                                @Nonnull final String resId, @Nonnull final String slotName, @Nonnull final VirtualFile virtualFile) {
        final DeploymentSlotPropertyView view = new DeploymentSlotPropertyView(project, sid, resId, slotName, virtualFile);
        view.onLoadWebAppProperty(sid, resId, slotName);
        return view;
    }

    private DeploymentSlotPropertyView(@Nonnull final Project project, @Nonnull final String sid,
                                       @Nonnull final String webAppId, @Nonnull final String slotName, @Nonnull final VirtualFile virtualFile) {
        super(project, sid, webAppId, slotName, virtualFile);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new DeploymentSlotPropertyViewPresenter();
    }
}
