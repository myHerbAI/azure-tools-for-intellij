/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.GradleProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.MavenProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.ProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.components.ModuleComboBox;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.plugins.gradle.model.ExternalDependency;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.text.Document;
import java.io.StringReader;
import java.util.Optional;

public class AzureSdkProjectDependencyPanel {
    public static final String DEPENDENCY_ADDED = "Dependency Added";
    public static final String ADD_DEPENDENCY = "Add Dependency";
    public static final String UPDATE_DEPENDENCY = "Update Dependency";

    private ModuleComboBox cbModule;
    private JButton btnAddDependency;
    private JTextPane paneMessage;
    private JPanel pnlRoot;
    @Getter
    @Setter
    private String version;
    @Getter
    @Setter
    private AzureSdkArtifactEntity pkg;

    private final Project project;
    private final IAzureMessager messager;

    public AzureSdkProjectDependencyPanel(@Nonnull final Project project) {
        this.project = project;
        this.messager = new DependencyNotificationMessager();
        $$$setupUI$$$();
        init();
    }

    public void setVisible(final boolean visible) {
        this.pnlRoot.setVisible(visible);
    }

    private void init() {
        cbModule.addItemListener(e -> onSelectModule());
        btnAddDependency.addActionListener(e -> onAddDependency());
    }

    @AzureOperation(name = "sdk.refresh_dependency", type = AzureOperation.Type.ACTION)
    public void onSelectModule() {
        OperationContext.action().setMessager(messager);
        btnAddDependency.setText("Loading...");
        btnAddDependency.setEnabled(false);
        AzureTaskManager.getInstance().runInBackground("Loading dependencies status", () -> {
            final ProjectModule module = cbModule.getValue();
            if (module instanceof MavenProjectModule) {
                final MavenArtifact mavenDependency = ((MavenProjectModule) module).getMavenDependency(pkg.getGroupId(), pkg.getArtifactId());
                final String currentVersion = Optional.ofNullable(mavenDependency).map(MavenArtifact::getVersion).orElse(null);
                updateDependencyStatus(module, currentVersion);
            } else if (module instanceof GradleProjectModule) {
                final ExternalDependency gradleDependency = ((GradleProjectModule) module).getGradleDependency(pkg.getGroupId(), pkg.getArtifactId());
                final String currentVersion = Optional.ofNullable(gradleDependency).map(ExternalDependency::getVersion).orElse(null);
                updateDependencyStatus(module, currentVersion);
            } else {
                btnAddDependency.setEnabled(false);
            }
        });
    }

    private void updateDependencyStatus(final ProjectModule module, final String currentVersion) {
        if (StringUtils.isEmpty(currentVersion)) {
            AzureMessager.getMessager().info(AzureString.format("Library %s was not found in module %s", pkg.getArtifactId(), module.getName()));
            btnAddDependency.setEnabled(true);
            btnAddDependency.setText(ADD_DEPENDENCY);
        } else {
            AzureMessager.getMessager().info(AzureString.format("Library %s was found in module %s with version %s",
                    pkg.getArtifactId(), module.getName(), currentVersion));
            final ComparableVersion current = new ComparableVersion(currentVersion);
            final ComparableVersion targetVersion = new ComparableVersion(version);
            btnAddDependency.setText(current.compareTo(targetVersion) >= 0 ? DEPENDENCY_ADDED : UPDATE_DEPENDENCY);
            btnAddDependency.setEnabled(current.compareTo(targetVersion) < 0);
        }
    }

    @AzureOperation(name = "sdk.add_dependency", type = AzureOperation.Type.ACTION)
    private void onAddDependency() {
        OperationContext.action().setMessager(messager);
        btnAddDependency.setText("Running...");
        btnAddDependency.setEnabled(false);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>("Add dependency", this::addDependency));
    }

    private void addDependency() {
        final ProjectModule module = cbModule.getValue();
        try {
            if (module instanceof MavenProjectModule) {
                DependencyUtils.addOrUpdateMavenDependency((MavenProjectModule) module, pkg, version);
            } else if (module instanceof GradleProjectModule) {
                DependencyUtils.addOrUpdateGradleDependency((GradleProjectModule) module, pkg, version);
            }
            btnAddDependency.setText(DEPENDENCY_ADDED);
            btnAddDependency.setEnabled(false);
        } catch (final Throwable t) {
            AzureMessager.getMessager().error(t);
            onSelectModule();
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbModule = new ModuleComboBox(this.project);
        cbModule.reloadItems();
    }

    private class DependencyNotificationMessager implements IAzureMessager {
        @Override
        public synchronized boolean show(IAzureMessage message) {
            paneMessage.setDocument(paneMessage.getEditorKit().createDefaultDocument());
            paneMessage.setText(message.getMessage().toString());
            return true;
        }

    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
