package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;

import javax.annotation.Nonnull;
import java.util.List;

public interface IConnectionAware extends RunConfiguration {
    @Deprecated
    default void addConnection(@Nonnull final Connection<?, ?> connection) {}

    default Module getModule() {
        return null;
    }

    @Nonnull
    default List<Connection<?, ?>> getConnections() {
        return ConnectionManager.getConnectionForRunConfiguration(this);
    }

    default boolean isConnectionEnabled() {
        return this.getBeforeRunTasks().stream().anyMatch(task -> task instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask);
    }
}
