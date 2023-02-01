/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.containerregistry.Repository;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ACRRepositoryTagComboBox extends AzureComboBox<Pair<String, OffsetDateTime>> {
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
        return ((Pair<String, OffsetDateTime>) item).getLeft();
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
    protected List<? extends Pair<String, OffsetDateTime>> loadItems() {
        if (Objects.nonNull(this.repository)) {
            return this.repository.listTagsWithDates();
        }
        return Collections.emptyList();
    }
}
