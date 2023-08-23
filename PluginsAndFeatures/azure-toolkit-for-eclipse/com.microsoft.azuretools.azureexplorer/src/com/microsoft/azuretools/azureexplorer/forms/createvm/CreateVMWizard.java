/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.azureexplorer.forms.createvm;

import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.compute.models.VirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachineSize;
import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.AzureCompute;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.azureexplorer.helpers.CreateStorageAccountTask;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMNode;
import org.eclipse.jface.wizard.Wizard;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_VM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.VM;

public class CreateVMWizard extends Wizard implements TelemetryProperties {
    private VMArmModule node;

    protected Subscription subscription;
    protected String name;
    protected String userName;
    protected String password;
    protected String certificate;
    protected String subnet;
    protected VirtualMachineSize size;

    private Region region;
    private Network virtualNetwork;
    private VirtualNetwork newNetwork;
    private boolean isNewNetwork;
    private String resourceGroupName;
    private boolean isNewResourceGroup;
    private VirtualMachineImage virtualMachineImage;
    private Object knownMachineImage;
    private boolean isKnownMachineImage;
    private StorageAccount storageAccount;
    private StorageAccountConfig newStorageAccount;
    private boolean withNewStorageAccount;
    private PublicIpAddress publicIpAddress;
    private boolean withNewPip;
    private AvailabilitySet availabilitySet;
    private boolean withNewAvailabilitySet;
    private NetworkSecurityGroup networkSecurityGroup;

	private ComputeManager computeManager;
	private NetworkManager networkManager;

    public CreateVMWizard(VMArmModule node) {
        this.node = node;
        setWindowTitle("Create new Virtual Machine");
    }

    @Override
    public void addPages() {
        addPage(new SubscriptionStep(this));
        addPage(new SelectImageStep(this));
        addPage(new MachineSettingsStep(this));
        addPage(new SettingsStep(this));
    }

    @Override
    public boolean performFinish() {
        Operation operation = TelemetryManager.createOperation(VM, CREATE_VM);
        AzureTaskManager.getInstance().runInBackground("Creating virtual machine " + name + "...", new Runnable() {
            @Override
            public void run() {
                try {
                    operation.start();
                    byte[] certData = new byte[0];
                    if (!certificate.isEmpty()) {
                        File certFile = new File(certificate);
                        if (certFile.exists()) {
                            try (FileInputStream certStream = new FileInputStream(certFile)){
                                certData = new byte[(int) certFile.length()];
                                if (certStream.read(certData) != certData.length) {
                                    throw new Exception("Unable to process certificate: "
                                        + "stream longer than informed size.");
                                }
                            } finally {
                            }
                        }
                    }
                    // create storage account when use choose to create new one
                    if (Objects.nonNull(newStorageAccount)) {
                        storageAccount = new CreateStorageAccountTask(newStorageAccount).execute();
                    }

                    //@TODO wangmi create vm
                    
                } catch (Exception e) {
                    EventUtil.logError(operation, ErrorType.userError, e, null, null);
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        public void run() {
                            PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), "Error Creating Virtual Machine", "An error occurred while trying to create the specified virtual machine", e);
                        }
                    });
                } finally {
                    operation.complete();
                }
            }
        });
        return true;
    }

    @Override
    public boolean canFinish() {
        return getContainer().getCurrentPage() instanceof SettingsStep;
    }

    public ComputeManager getComputeManager() {
        return computeManager;
    }
    
    public NetworkManager getNetworkManager() {
    	return networkManager;
    }

    public void setSubscription(Subscription subscription) {
        try {
            this.subscription = subscription;
            final String subscriptionId = subscription.getId();
            computeManager = Azure.az(AzureCompute.class).forSubscription(subscriptionId).getRemote();
            networkManager = Azure.az(AzureNetwork.class).forSubscription(subscriptionId).getRemote();
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().showException(ex.getMessage(), ex, "Error selecting subscription", true, false);
        }
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Network getVirtualNetwork() {
        return virtualNetwork;
    }

    public void setVirtualNetwork(Network virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
    }

    public VirtualNetwork getNewNetwork() {
        return newNetwork;
    }

    public void setNewNetwork(VirtualNetwork newNetwork) {
        this.newNetwork = newNetwork;
    }

    public boolean isNewNetwork() {
        return isNewNetwork;
    }

    public void setNewNetwork(boolean isNewNetwork) {
        this.isNewNetwork = isNewNetwork;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isNewResourceGroup() {
        return isNewResourceGroup;
    }

    public void setNewResourceGroup(boolean isNewResourceGroup) {
        this.isNewResourceGroup = isNewResourceGroup;
    }

    public VirtualMachineImage getVirtualMachineImage() {
        return virtualMachineImage;
    }

    public void setVirtualMachineImage(VirtualMachineImage virtualMachineImage) {
        this.virtualMachineImage = virtualMachineImage;
    }

    public Object getKnownMachineImage() {
        return knownMachineImage;
    }

    public void setKnownMachineImage(Object knownMachineImage) {
        this.knownMachineImage = knownMachineImage;
    }

    public boolean isKnownMachineImage() {
        return isKnownMachineImage;
    }

    public void setKnownMachineImage(boolean isKnownMachineImage) {
        this.isKnownMachineImage = isKnownMachineImage;
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    public void setStorageAccount(StorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public StorageAccountConfig getNewStorageAccount() {
        return newStorageAccount;
    }

    public void setNewStorageAccount(StorageAccountConfig newStorageAccount) {
        this.newStorageAccount = newStorageAccount;
    }

    public boolean isWithNewStorageAccount() {
        return withNewStorageAccount;
    }

    public void setWithNewStorageAccount(boolean withNewStorageAccount) {
        this.withNewStorageAccount = withNewStorageAccount;
    }

    public PublicIpAddress getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(PublicIpAddress publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public boolean isWithNewPip() {
        return withNewPip;
    }

    public void setWithNewPip(boolean withNewPip) {
        this.withNewPip = withNewPip;
    }

    public NetworkSecurityGroup getNetworkSecurityGroup() {
        return networkSecurityGroup;
    }

    public void setNetworkSecurityGroup(NetworkSecurityGroup networkSecurityGroup) {
        this.networkSecurityGroup = networkSecurityGroup;
    }

    public AvailabilitySet getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(AvailabilitySet availabilitySet) {
        this.availabilitySet = availabilitySet;
    }

    public boolean isWithNewAvailabilitySet() {
        return withNewAvailabilitySet;
    }

    public void setWithNewAvailabilitySet(boolean withNewAvailabilitySet) {
        this.withNewAvailabilitySet = withNewAvailabilitySet;
    }

    public VirtualMachineSize getSize() {
        return size;
    }

    public void setSize(VirtualMachineSize size) {
        this.size = size;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if(this.getSubnet() != null) properties.put("Size", this.getSubnet());
        if(this.getSubscription() != null) {
            properties.put("SubscriptionName", this.getSubscription().getName());
            properties.put("SubscriptionId", this.getSubscription().getId());
        }
        if(this.getName() != null) properties.put("Name", this.getName());
        if(this.getRegion() != null) properties.put("Region", this.getRegion().getLabel());
        if(this.getSize() != null) properties.put("Size", this.getSize().name());

        return properties;
    }


}
