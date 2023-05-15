package com.microsoft.azure.toolkit.intellij.containerapps.streaminglog;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Replica;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ReplicaContainer;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Revision;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class ContainerSelectionDialog extends DialogWrapper {
    private JPanel rootPanel;
    private AzureComboBox<Revision> revisionComboBox;
    private AzureComboBox<Replica> replicaComboBox;
    private AzureComboBox<ReplicaContainer> containerComboBox;
    private final ContainerApp containerApp;
    private final Project project;
    private static final String GO_TO_REVISION_MANAGEMENT = "You need at least one instance to view logs. To change settings, go to Revision management in portal.";

    public ContainerSelectionDialog(@Nullable final Project project, ContainerApp containerApp) {
        super(project, false);
        this.project = project;
        this.containerApp = containerApp;
        setTitle("Select Revision");
        init();
        initListeners();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.rootPanel;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        AzureTaskManager.getInstance().runInBackground("Start Streaming Log", () -> startStreamingLog(project, containerComboBox.getValue()));
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (revisionComboBox.getItems().size() == 0) {
            return new ValidationInfo(GO_TO_REVISION_MANAGEMENT, revisionComboBox);
        }
        if (Objects.isNull(revisionComboBox.getValue())) {
            return new ValidationInfo("Revision is required", revisionComboBox);
        }
        if (replicaComboBox.getItems().size() == 0) {
            return new ValidationInfo(GO_TO_REVISION_MANAGEMENT, replicaComboBox);
        }
        if (Objects.isNull(replicaComboBox.getValue())) {
            return new ValidationInfo("Replica is required", replicaComboBox);
        }
        if (containerComboBox.getItems().size() == 0) {
            return new ValidationInfo(GO_TO_REVISION_MANAGEMENT, containerComboBox);
        }
        if (Objects.isNull(containerComboBox.getValue())) {
            return new ValidationInfo("Container is required", containerComboBox);
        }
        return null;
    }

    private void startStreamingLog(Project project, ReplicaContainer container) {
        final Replica replica = container.getParent();
        final Revision revision = replica.getParent();
        final ContainerApp app = revision.getParent();
        final String resourceName = String.format("%s-%s", replica.getName(), container.getName());
        final String resourceId = String.format("%s/revisionManagement/%s", app.getId(), resourceName);
        final Flux<String> logs = container.streamingLogs(true, 20);
        AzureTaskManager.getInstance().runLater(() -> StreamingLogsManager.getInstance().showStreamingLog(project, resourceId, resourceName, logs));
    }

    private void initListeners() {
        revisionComboBox.setItemsLoader(containerApp::getRevisions);
        revisionComboBox.addItemListener(e -> replicaComboBox.reloadItems());
        replicaComboBox.setItemsLoader(() -> Optional.ofNullable(revisionComboBox.getValue())
            .map(Revision::getReplicas)
            .orElse(Collections.emptyList()));
        replicaComboBox.addItemListener(e -> containerComboBox.reloadItems());
        containerComboBox.setItemsLoader(() -> Optional.ofNullable(replicaComboBox.getValue())
            .map(r -> r.getContainerModule().list())
            .orElse(Collections.emptyList()));
    }
}
