/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.storage.azurite;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class AzuriteTaskProvider extends BeforeRunTaskProvider<AzuriteTaskProvider.AzuriteBeforeRunTask> {
    private static final Key<AzuriteBeforeRunTask> ID = Key.create("AzuriteTaskProviderId");

    @Getter
    public Key<AzuriteBeforeRunTask> id = ID;

    @Override
    public @Nullable
    Icon getTaskIcon(AzuriteBeforeRunTask task) {
        return AzuriteService.ICON;
    }

    @Nullable
    @Override
    public AzuriteBeforeRunTask createTask(@Nonnull RunConfiguration runConfiguration) {
        return new AzuriteBeforeRunTask();
    }

    @Override
    public boolean executeTask(@Nonnull DataContext context, @Nonnull RunConfiguration configuration, @Nonnull ExecutionEnvironment environment, @Nonnull AzuriteBeforeRunTask task) {
        return task.startAzurite(configuration);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getName() {
        return "Run Azurite";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription(AzuriteBeforeRunTask task) {
        return task.getDescription();
    }

    @Getter
    @Setter
    public static class AzuriteBeforeRunTask extends BeforeRunTask<AzuriteBeforeRunTask> {
        protected AzuriteBeforeRunTask() {
            super(ID);
        }

        public boolean startAzurite(@Nonnull final RunConfiguration configuration) {
            AzureTaskManager.getInstance().runAndWait(() -> {
                final AzResource.FormalStatus formalStatus = AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT.getFormalStatus();
                if (!formalStatus.isRunning()) {
                    final boolean result = AzuriteService.getInstance().startAzurite(configuration.getProject());
                    if (result) {
                        addStopAzuriteListener(configuration);
                    }
                }
            });
            return true;
        }

        private String getDescription() {
            return "Run Azurite";
        }

        public static void addStopAzuriteListener(@Nonnull final RunConfiguration runConfiguration) {
            final Project project = runConfiguration.getProject();
            final MessageBusConnection messageBusConnection = project.getMessageBus().connect();
            messageBusConnection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
                @Override
                public void processTerminated(@Nonnull String executorId, @Nonnull ExecutionEnvironment env, @Nonnull ProcessHandler handler, int exitCode) {
                    Optional.ofNullable(env.getRunnerAndConfigurationSettings()).ifPresent(settings -> {
                        if (Objects.equals(settings.getConfiguration(), runConfiguration)) {
                            AzureTaskManager.getInstance().runAndWait(() -> AzuriteService.getInstance().stopAzurite());
                        }
                    });
                    messageBusConnection.disconnect();
                }
            });
        }
    }
}

