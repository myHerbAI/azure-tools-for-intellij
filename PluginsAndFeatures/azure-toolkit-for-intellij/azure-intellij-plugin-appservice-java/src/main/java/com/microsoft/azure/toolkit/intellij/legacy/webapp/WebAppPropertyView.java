/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

import javax.annotation.Nonnull;

public class WebAppPropertyView extends WebAppBasePropertyView {
    private static final String ID = "com.microsoft.intellij.helpers.webapp.WebAppBasePropertyView";

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppBasePropertyView create(@Nonnull final Project project, @Nonnull final String sid,
                                                @Nonnull final String webAppId, @Nonnull final VirtualFile virtualFile) {
        final WebAppPropertyView view = new WebAppPropertyView(project, sid, webAppId, virtualFile);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }

    private WebAppPropertyView(@Nonnull final Project project, @Nonnull final String sid,
                               @Nonnull final String webAppId, @Nonnull final VirtualFile virtualFile) {
        super(project, sid, webAppId, null, virtualFile);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new WebAppPropertyViewPresenter();
    }
}
