/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.StorageAccountAccessKey;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.api.PostListKeysResponse;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import rx.Observable;

public class ADLSGen2StorageAccount extends HDStorageAccount implements ILogger {
    public final static String DefaultScheme = "abfs";

    public ADLSGen2StorageAccount(IClusterDetail clusterDetail, String fullStorageBlobName, String key, boolean isDefault, String defaultFileSystem, String scheme) {
        super(clusterDetail, fullStorageBlobName, key, isDefault, defaultFileSystem);
        this.scheme = scheme;
        key = getAccessKeyList(clusterDetail.getSubscription())
                .toBlocking()
                .firstOrDefault(new StorageAccountAccessKey())
                .getValue();

        this.setPrimaryKey(key);
    }

    public ADLSGen2StorageAccount(IClusterDetail clusterDetail, String fullStorageBlobName, boolean isDefault, String defaultFileSystem) {
        super(clusterDetail, fullStorageBlobName, null, isDefault, defaultFileSystem);
        this.scheme = DefaultScheme;
    }

    public String getStorageRootPath() {
        return String.format("%s://%s@%s", this.getscheme(), this.getDefaultContainer(), this.getFullStorageBlobName());
    }

    @Override
    public StorageAccountType getAccountType() {
        return StorageAccountType.ADLSGen2;
    }

    private Observable<StorageAccountAccessKey> getAccessKeyList(Subscription subscription) {
        final String sid = subscription.getId();
        return Observable.fromCallable(() -> Azure.az(AzureStorageAccount.class))
                .flatMap(azure -> Observable.from(azure.accounts(getSubscriptionId()).list()))
                .filter(account -> account.getName().equals(getName()))
                .map(AbstractAzResource::getResourceGroupName)
                .first()
                .doOnNext(rgName -> log().info(String.format("Finish getting storage account %s resource group name %s", getName(), rgName)))
                .flatMap(rgName -> new AzureHttpObservable(subscription, "2018-07-01").post(String.format("https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s/listKeys",
                    sid, rgName, getName()), null, null, null, PostListKeysResponse.class))
                .flatMap(keyList -> Observable.from(keyList.getKeys()));
    }
}
