package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.module.Module;

import javax.annotation.Nonnull;

public interface IConnectionAware {
    void addConnection(@Nonnull final Connection<?, ?> connection);

    default Module getModule() {
        return null;
    }
}
