/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector;

@FunctionalInterface
public interface ConnectionProvider {
    <R, C> Connection<R, C> define(String id, Resource<R> resource, Resource<C> consumer, ConnectionDefinition<R, C> definition);
}
