/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage.implementation;

import com.microsoft.azure.hdinsight.sdk.cluster.ClusterIdentity;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSCertificateInfo;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azure.hdinsight.sdk.storage.model.IHDIStorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import java.net.URI;

public class ADLSStorageAccount implements IHDIStorageAccount {
    private final String name;
    private boolean isDefaultStorageAccount;
    private final String defaultRootFolderPath;
    private final IClusterDetail clusterDetail;
    private final ClusterIdentity clusterIdentity;
    private ADLSCertificateInfo certificateInfo;
    private final String defaultStorageSchema;

    public ADLSStorageAccount(IClusterDetail clusterDetail, String name, boolean isDefault, String defaultRootPath, ClusterIdentity clusterIdentity, String storageSchema) {
        this.name = name;
        this.isDefaultStorageAccount = isDefault;
        this.defaultRootFolderPath = defaultRootPath;
        this.clusterDetail = clusterDetail;
        this.clusterIdentity = clusterIdentity;
        this.defaultStorageSchema = storageSchema;
    }

    public ADLSStorageAccount(IClusterDetail clusterDetail, boolean isDefault, ClusterIdentity clusterIdentity, URI rootURI) {
        this.name = getAccountName(rootURI);
        this.isDefaultStorageAccount = isDefault;
        this.defaultRootFolderPath = rootURI.getPath();
        this.clusterDetail = clusterDetail;
        this.clusterIdentity = clusterIdentity;
        this.defaultStorageSchema = rootURI.getScheme();
    }

    @Override
    public String getSubscriptionId() {
        return this.clusterDetail.getSubscription().getId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StorageAccountType getAccountType() {
        return StorageAccountType.ADLS;
    }

    @Override
    public String getDefaultContainerOrRootPath() {
        return defaultRootFolderPath;
    }

    @Override
    public String getDefaultStorageSchema(){
        return defaultStorageSchema;
    }

    @NotNull
    public ClusterIdentity getClusterIdentity() {
        return this.clusterIdentity;
    }

    @NotNull
    public ADLSCertificateInfo getCertificateInfo() throws HDIException {
        if (this.certificateInfo == null) {
            try {
                this.certificateInfo = new ADLSCertificateInfo(this.clusterIdentity);
                return certificateInfo;
            } catch (Exception e) {
                throw  new HDIException("get ADLS certificate error", e.getMessage());
            }
        } else {
            return this.certificateInfo;
        }
    }

    @NotNull
    private String getAccountName(URI root) {
        //get xxx from host name xxx.azuredatalakestore.net
        String host = root.getHost();
        return host.substring(0, host.indexOf("."));
    }
}
