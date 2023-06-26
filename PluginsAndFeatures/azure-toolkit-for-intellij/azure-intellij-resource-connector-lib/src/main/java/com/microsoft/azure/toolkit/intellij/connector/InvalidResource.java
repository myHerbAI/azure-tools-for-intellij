/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector;

import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;

@RequiredArgsConstructor
public class InvalidResource<T> implements Resource<T> {

    private final ResourceDefinition<T> definition;

    @Nonnull
    @Override
    public ResourceDefinition<T> getDefinition() {
        return this.definition;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public T getData() {
        return null;
    }

    @Override
    public String getDataId() {
        return null;
    }

    @Override
    public String getName() {
        return "Invalid Resource";
    }

    @Override
    public boolean isValidResource() {
        return false;
    }
}
