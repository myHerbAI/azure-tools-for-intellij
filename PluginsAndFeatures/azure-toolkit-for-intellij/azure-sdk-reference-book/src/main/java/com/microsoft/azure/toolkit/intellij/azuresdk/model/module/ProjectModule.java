/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.azuresdk.model.module;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public interface ProjectModule {

    String getName();

    Project getProject();

    Icon getIcon();

    ComparableVersion getDependencyVersion(@Nullable final AzureSdkArtifactEntity entity);

    default boolean isDependencyExists(@Nullable final AzureSdkArtifactEntity entity) {
        return Objects.nonNull(getDependencyVersion(entity));
    }

    default boolean isDependencyUpToDate(@Nullable final AzureSdkArtifactEntity entity, final String version) {
        final ComparableVersion current = Optional.ofNullable(getDependencyVersion(entity)).orElseGet(() -> new ComparableVersion(StringUtils.EMPTY));
        final ComparableVersion targetVersion = new ComparableVersion(version);
        return current.compareTo(targetVersion) >= 0;
    }

    public static enum Type {
        Maven,
        Gradle
    }
}
