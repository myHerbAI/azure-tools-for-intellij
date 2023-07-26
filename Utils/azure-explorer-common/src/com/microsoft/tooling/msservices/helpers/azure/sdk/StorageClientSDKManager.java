/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers.azure.sdk;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.util.logging.ClientLogger;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerAccessPolicies;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobContainerItemProperties;
import com.azure.storage.common.implementation.connectionstring.StorageAuthenticationSettings;
import com.azure.storage.common.implementation.connectionstring.StorageConnectionString;
import com.azure.storage.common.implementation.connectionstring.StorageEndpoint;
import com.google.common.base.Strings;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.StorageAccoutUtils;
import com.microsoft.tooling.msservices.helpers.CallableSingleArg;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

public class StorageClientSDKManager {
    private static StorageClientSDKManager apiManager;

    private StorageClientSDKManager() {
    }

    @NotNull
    public static StorageClientSDKManager getManager() {
        if (apiManager == null) {
            apiManager = new StorageClientSDKManager();
        }

        return apiManager;
    }

    @NotNull
    public ClientStorageAccount getStorageAccount(@NotNull String connectionString) {
        final ClientLogger logger = new ClientLogger(StorageClientSDKManager.class);
        final StorageConnectionString connection = StorageConnectionString.create(connectionString, logger);
        final ClientStorageAccount storageAccount = new ClientStorageAccount(connection.getAccountName());

        final StorageAuthenticationSettings authSettings = connection.getStorageAuthSettings();
        storageAccount.setPrimaryKey(authSettings.getAccount().getAccessKey());
        if (StringUtils.containsIgnoreCase(connectionString, ClientStorageAccount.DEFAULT_ENDPOINTS_PROTOCOL_KEY)) {
            final String protocol = Stream.of(connection.getBlobEndpoint(), connection.getQueueEndpoint(), connection.getTableEndpoint())
                    .map(StorageEndpoint::getPrimaryUri)
                    .filter(StringUtils::isNoneBlank)
                    .findFirst()
                    .map(this::getProtocol).orElse(null);
            storageAccount.setProtocol(protocol);
            storageAccount.setUseCustomEndpoints(false);
        } else {
            storageAccount.setUseCustomEndpoints(true);
            storageAccount.setBlobsUri(connection.getBlobEndpoint().getPrimaryUri());
            storageAccount.setQueuesUri(connection.getQueueEndpoint().getPrimaryUri());
            storageAccount.setTablesUri(connection.getTableEndpoint().getPrimaryUri());
        }

        return storageAccount;
    }

    private String getProtocol(@Nonnull final String s) {
        try {
            return new URL(s).getProtocol();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @NotNull
    public List<BlobContainer> getBlobContainers(@NotNull String connectionString)
            throws AzureCmdException {
        return getBlobContainers(connectionString, null);
    }

    public List<BlobContainer> getBlobContainers(@NotNull String connectionString, @Nullable Duration timeouts)
            throws AzureCmdException {
        List<BlobContainer> bcList = new ArrayList<>();

        try {
            BlobServiceClient client = getCloudBlobClient(connectionString);
            for (BlobContainerItem container : client.listBlobContainers(null, timeouts)) {
                final BlobContainerClient containerClient = client.getBlobContainerClient(container.getName());
                String uri = containerClient.getBlobContainerUrl();
                String eTag = "";
                Calendar lastModified = new GregorianCalendar();
                final BlobContainerItemProperties properties = container.getProperties();

                if (properties != null) {
                    eTag = Strings.nullToEmpty(properties.getETag());

                    if (properties.getLastModified() != null) {
                        lastModified.setTime(Date.from(properties.getLastModified().toInstant()));
                    }
                }

                String publicReadAccessType = "";
                BlobContainerAccessPolicies blobContainerPermissions = containerClient.getAccessPolicy();

                if (blobContainerPermissions != null && blobContainerPermissions.getBlobAccessType() != null) {
                    publicReadAccessType = blobContainerPermissions.getBlobAccessType().toString();
                }

                bcList.add(new BlobContainer(Strings.nullToEmpty(container.getName()),
                        uri,
                        eTag,
                        lastModified,
                        publicReadAccessType));
            }

            return bcList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Blob Container list", t);
        }

    }

    public void uploadBlobFileContent(@NotNull String connectionString,
                                      @NotNull BlobContainer blobContainer,
                                      @NotNull String filePath,
                                      @NotNull InputStream content,
                                      CallableSingleArg<Void, Long> processBlock,
                                      long maxBlockSize,
                                      long length)
            throws AzureCmdException {
        try {
            BlobServiceClient client = getCloudBlobClient(connectionString);
            String containerName = blobContainer.getName();

            BlobContainerClient container = client.getBlobContainerClient(containerName);
            final BlobClient blob = container.getBlobClient(filePath);
            blob.upload(content, length);
        } catch (Throwable t) {
            throw new AzureCmdException("Error uploading the Blob File content", t);
        }
    }

    public static String getEndpointSuffix() {
        return StorageAccoutUtils.getEndpointSuffix();
    }

    @NotNull
    public static BlobServiceClient getCloudBlobClient(@NotNull String connectionString) {
        return new BlobServiceClientBuilder().httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)).connectionString(connectionString).buildClient();
    }
}
