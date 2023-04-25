/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.containerregistry.Repository;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ACRRepositoryTagComboBox extends AzureComboBox<Tag> {
    private Repository repository;

    @Override
    public String getLabel() {
        return "Tag";
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((Tag) item).getName();
    }

    public void setRepository(Repository repository) {
        if (Objects.equals(repository, this.repository)) {
            return;
        }
        this.repository = repository;
        if (repository == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nonnull
    @Override
    protected List<Tag> loadItems() {
        if (Objects.nonNull(this.repository)) {
            return this.repository.getArtifactModule().list().stream()
                .flatMap(i -> i.getTagModule().list().stream()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
