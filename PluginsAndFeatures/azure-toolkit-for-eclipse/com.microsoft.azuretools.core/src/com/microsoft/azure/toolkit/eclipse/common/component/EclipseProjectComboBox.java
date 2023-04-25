/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.WAPropertyTester;

public class EclipseProjectComboBox extends AzureComboBox<IProject> {

    public EclipseProjectComboBox(Composite parent) {
        super(parent, false);
    }

    @Override
    public String getItemText(Object item) {
        // TODO Auto-generated method stub
        return item instanceof IProject ? ((IProject) item).getName() : super.getItemText(item);
    }

    @Override
    @AzureOperation(name = "internal/common.list_regions.subscription", // TODO: add properties
            params = { "this.subscription.getId()" })
    protected List<? extends IProject> loadItems() {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        return Optional.ofNullable(workspace).map(IWorkspace::getRoot).map(IWorkspaceRoot::getProjects)
                .map(projects -> Arrays.asList(projects).stream().filter(project -> {
                    try {
                        return WAPropertyTester.isWebProj(project) || MavenUtils.isMavenProject(project);
                    } catch (CoreException e) {
                        return false;
                    }
                }).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

}