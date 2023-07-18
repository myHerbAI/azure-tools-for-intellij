package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DotEnvBeforeRunTaskProvider;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;

import javax.annotation.Nonnull;
import java.util.*;

public interface IConnectionAware extends RunConfiguration {

    default Module getModule() {
        return null;
    }

    @Nonnull
    @Deprecated
    default List<Connection<?, ?>> getConnections() {
        return AzureModule.createIfSupport(this)
            .map(AzureModule::getDefaultProfile)
            .map(Profile::getConnections)
            .orElse(Collections.emptyList());
    }

    default Map<String, String> getEnvironmentVariables() {
        final Map<String, String> vars = new HashMap<>();
        this.getBeforeRunTasks().stream()
            .filter(t -> t instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask)
            .map(t -> (DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask) t)
            .flatMap(t -> t.loadEnv().stream())
            .forEach(p -> vars.put(p.getKey(), p.getValue()));
        return vars;
    }

    @Deprecated
    default DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask getLoadDotEnvBeforeRunTask() {
        return (DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask) this.getBeforeRunTasks().stream()
            .filter(task -> task instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask).findAny().orElse(null);
    }

    @Deprecated
    default boolean isConnectionEnabled() {
        return Objects.nonNull(getLoadDotEnvBeforeRunTask());
    }
}
