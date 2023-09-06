package com.microsoft.azure.hdinsight.sdk.cluster;

import com.azure.core.http.rest.Response;
import com.azure.core.management.Region;
import com.azure.core.management.SystemData;
import com.azure.core.util.Context;
import com.azure.resourcemanager.hdinsight.fluent.models.ClusterInner;
import com.azure.resourcemanager.hdinsight.models.*;
import com.azure.resourcemanager.hdinsight.models.ClusterIdentity;

import java.util.List;
import java.util.Map;

public class SDKAdditionalCluster implements Cluster {

    private String id;
    private String name;
    private String type;
    private String location;
    private Map<String, String> tags;
    private String etag;
    private List<String> zones;
    private ClusterGetProperties clusterGetProperties;
    private ClusterIdentity identity;
    private SystemData systemData;
    private Region region;
    private String regionName;
    private String resourceGroupName;

    @Override
    public ClusterInner innerModel() {
        return null;
    }

    @Override
    public Update update() {
        return null;
    }

    @Override
    public Cluster refresh() {
        return null;
    }

    @Override
    public Cluster refresh(Context context) {
        return null;
    }

    @Override
    public void rotateDiskEncryptionKey(ClusterDiskEncryptionParameters clusterDiskEncryptionParameters) {

    }

    @Override
    public void rotateDiskEncryptionKey(ClusterDiskEncryptionParameters clusterDiskEncryptionParameters, Context context) {

    }

    @Override
    public Response<GatewaySettings> getGatewaySettingsWithResponse(Context context) {
        return null;
    }

    @Override
    public GatewaySettings getGatewaySettings() {
        return null;
    }

    @Override
    public void updateGatewaySettings(UpdateGatewaySettingsParameters updateGatewaySettingsParameters) {

    }

    @Override
    public void updateGatewaySettings(UpdateGatewaySettingsParameters updateGatewaySettingsParameters, Context context) {

    }

    @Override
    public void updateIdentityCertificate(UpdateClusterIdentityCertificateParameters updateClusterIdentityCertificateParameters) {

    }

    @Override
    public void updateIdentityCertificate(UpdateClusterIdentityCertificateParameters updateClusterIdentityCertificateParameters, Context context) {

    }

    @Override
    public void executeScriptActions(ExecuteScriptActionParameters executeScriptActionParameters) {

    }

    @Override
    public void executeScriptActions(ExecuteScriptActionParameters executeScriptActionParameters, Context context) {

    }


    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public void setZones(List<String> zones) {
        this.zones = zones;
    }

    public void setClusterGetProperties(ClusterGetProperties clusterGetProperties) {
        this.clusterGetProperties = clusterGetProperties;
    }

    public void setIdentity(ClusterIdentity identity) {
        this.identity = identity;
    }

    public void setSystemData(SystemData systemData) {
        this.systemData = systemData;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String location() {
        return location;
    }

    @Override
    public Map<String, String> tags() {
        return tags;
    }

    @Override
    public String etag() {
        return etag;
    }

    @Override
    public List<String> zones() {
        return zones;
    }

    @Override
    public ClusterGetProperties properties() {
        return clusterGetProperties;
    }

    @Override
    public ClusterIdentity identity() {
        return identity;
    }

    @Override
    public SystemData systemData() {
        return systemData;
    }

    @Override
    public Region region() {
        return region;
    }

    @Override
    public String regionName() {
        return regionName;
    }

    @Override
    public String resourceGroupName() {
        return resourceGroupName;
    }
}
