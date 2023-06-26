/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.cosmos;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraTable;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCollection;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDocument;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainer;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDocument;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CosmosNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "Azure Cosmos DB";
    private static final String ICON = AzureIcons.Cosmos.MODULE.getIconPath();

    @Nullable
    @Override
    public AzureCosmosService getRoot() {
        return Azure.az(AzureCosmosService.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureCosmosService || data instanceof CosmosDBAccount || data instanceof MongoDatabase || data instanceof MongoCollection ||
            data instanceof CassandraKeyspace || data instanceof CassandraTable || data instanceof SqlDatabase || data instanceof SqlContainer ||
            data instanceof SqlDocument || data instanceof MongoDocument;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureCosmosService) {
            final Function<AzureCosmosService, List<CosmosDBAccount>> listFunction = acs -> acs.list().stream().flatMap(m -> m.databaseAccounts().list().stream())
                .collect(Collectors.toList());
            return new AzureCosmosServiceNode((AzureCosmosService) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(CosmosActionsContributor.SERVICE_ACTIONS)
                .addChildren(listFunction, (account, serviceNode) -> this.createNode(account, serviceNode, manager));
        } else if (data instanceof SqlCosmosDBAccount) {
            return new CosmosDBAccountNode<>((SqlCosmosDBAccount) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.SQL_ACCOUNT_ACTIONS)
                .addChildren(account -> account.sqlDatabases().list(), (database, accountNode) -> this.createNode(database, accountNode, manager))
                .withMoreChildren(account -> account.sqlDatabases().hasMoreResources(), account -> account.sqlDatabases().loadMoreResources());
        } else if (data instanceof MongoCosmosDBAccount) {
            return new CosmosDBAccountNode<>((MongoCosmosDBAccount) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.MONGO_ACCOUNT_ACTIONS)
                .addChildren(account -> account.mongoDatabases().list(), (database, accountNode) -> this.createNode(database, accountNode, manager))
                .withMoreChildren(account -> account.mongoDatabases().hasMoreResources(), account -> account.mongoDatabases().loadMoreResources());
        } else if (data instanceof CassandraCosmosDBAccount) {
            return new CosmosDBAccountNode<>((CassandraCosmosDBAccount) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.CASSANDRA_ACCOUNT_ACTIONS)
                .addChildren(account -> account.keySpaces().list(), (keyspace, accountNode) -> this.createNode(keyspace, accountNode, manager))
                .withMoreChildren(account -> account.keySpaces().hasMoreResources(), account -> account.keySpaces().loadMoreResources());
        } else if (data instanceof CosmosDBAccount) {
            // for other cosmos db account (table/graph...)
            return new CosmosDBAccountNode<>((CosmosDBAccount) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.ACCOUNT_ACTIONS)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL);
        } else if (data instanceof MongoDatabase) {
            return new AzResourceNode<>((MongoDatabase) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.MONGO_DATABASE_ACTIONS)
                .addChildren(database -> database.collections().list(), (collection, databaseNode) -> this.createNode(collection, databaseNode, manager))
                .withMoreChildren(database -> database.collections().hasMoreResources(), database -> database.collections().loadMoreResources());
        } else if (data instanceof MongoCollection) {
            return new AzResourceNode<>((MongoCollection) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.MONGO_COLLECTION_ACTIONS)
                .addChildren(collection -> collection.getDocumentModule().list(), (document, collectionNode) -> this.createNode(document, collectionNode, manager))
                .withMoreChildren(collection -> collection.getDocumentModule().hasMoreResources(), collection -> collection.getDocumentModule().loadMoreResources());
        } else if (data instanceof SqlDatabase) {
            return new AzResourceNode<>((SqlDatabase) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.SQL_DATABASE_ACTIONS)
                .addChildren(database -> database.containers().list(), (container, databaseNode) -> this.createNode(container, databaseNode, manager))
                .withMoreChildren(database -> database.containers().hasMoreResources(), database -> database.containers().loadMoreResources());
        } else if (data instanceof SqlContainer) {
            return new AzResourceNode<>((SqlContainer) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.SQL_CONTAINER_ACTIONS)
                .addChildren(container -> container.getDocumentModule().list(), (document, containerNode) -> this.createNode(document, containerNode, manager))
                .withMoreChildren(container -> container.getDocumentModule().hasMoreResources(), container -> container.getDocumentModule().loadMoreResources());
        } else if (data instanceof CassandraKeyspace) {
            return new AzResourceNode<>((CassandraKeyspace) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.CASSANDRA_KEYSPACE_ACTIONS)
                .addChildren(keyspace -> keyspace.tables().list(), (table, keyspaceNode) -> this.createNode(table, keyspaceNode, manager))
                .withMoreChildren(keyspace -> keyspace.tables().hasMoreResources(), keyspace -> keyspace.tables().loadMoreResources());
        } else if (data instanceof CassandraTable) {
            return new AzResourceNode<>((CassandraTable) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(CosmosActionsContributor.CASSANDRA_TABLE_ACTIONS)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL);
        } else if (data instanceof MongoDocument) {
            return new AzResourceNode<>((MongoDocument) data)
                .withIcon(AzureIcons.Cosmos.DOCUMENT)
                .withLabel(MongoDocument::getDocumentDisplayName)
                .withDescription(doc -> StringUtils.isEmpty(doc.getSharedKey()) ? StringUtils.EMPTY : doc.getSharedKey())
                .withActions(CosmosActionsContributor.COSMOS_DOCUMENT_ACTIONS)
                .onDoubleClicked(CosmosActionsContributor.OPEN_DOCUMENT);
        } else if (data instanceof SqlDocument) {
            return new AzResourceNode<>((SqlDocument) data)
                .withIcon(AzureIcons.Cosmos.DOCUMENT)
                .withLabel(SqlDocument::getDocumentDisplayName)
                .withDescription(doc -> StringUtils.isEmpty(doc.getDocumentPartitionKey()) ? StringUtils.EMPTY : doc.getDocumentPartitionKey())
                .withActions(CosmosActionsContributor.COSMOS_DOCUMENT_ACTIONS)
                .onDoubleClicked(CosmosActionsContributor.OPEN_DOCUMENT);
        }
        return null;
    }

    static class AzureCosmosServiceNode extends AzServiceNode<AzureCosmosService> {

        public AzureCosmosServiceNode(@Nonnull AzureCosmosService service) {
            super(service);
        }

        @Override
        protected void onEvent(AzureEvent event) {
            final Object source = event.getSource();
            if (source instanceof AzService && source.equals(this.getValue())) {
                this.refreshChildrenLater();
            }
        }
    }

    static class CosmosDBAccountNode<T extends CosmosDBAccount> extends AzResourceNode<T> {

        private static final AzureResourceIconProvider<CosmosDBAccount> COSMOS_ICON_PROVIDER = new AzureResourceIconProvider<CosmosDBAccount>()
            .withModifier(CosmosDBAccountNode::getAPIModifier);

        public CosmosDBAccountNode(@Nonnull T resource) {
            super(resource);
            this.withDescription(account -> account.getFormalStatus().isRunning() ?
                Optional.ofNullable(account.getKind()).map(DatabaseAccountKind::getValue).orElse("Unknown") : account.getStatus());
            this.withIcon(COSMOS_ICON_PROVIDER::getIcon);
        }

        @Nullable
        private static AzureIcon.Modifier getAPIModifier(@Nonnull CosmosDBAccount resource) {
            final DatabaseAccountKind kind = resource.getKind();
            return Objects.isNull(kind) ? null : new AzureIcon.Modifier(StringUtils.lowerCase(kind.getValue()), AzureIcon.ModifierLocation.BOTTOM_LEFT);
        }
    }
}
