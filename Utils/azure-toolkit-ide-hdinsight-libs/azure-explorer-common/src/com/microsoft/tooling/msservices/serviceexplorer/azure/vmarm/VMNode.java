/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMNode extends RefreshableNode implements TelemetryProperties {
    private static final String RUNNING_STATUS = "PowerState/running";
    private static final String STOPPED = "stopped";

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, ResourceId.fromString(this.virtualMachine.id()).subscriptionId());
        return properties;
    }

    @Override
    public @Nullable AzureIcon getIconSymbol() {
        final boolean running = isRunning();
        return running ? AzureIcons.VirtualMachine.RUNNING : AzureIcons.VirtualMachine.STOPPED;
    }

    @AzureOperation(name = "user/vm.delete_vm.vm", params = {"this.virtualMachine.name()"})
    private void delete() {
        virtualMachine.delete();
    }

    @AzureOperation(name = "user/vm.start_vm.vm", params = {"this.virtualMachine.name()"})
    private void start() {
        virtualMachine.start();
        refreshItems();
    }

    @AzureOperation(name = "user/vm.restart_vm.vm", params = {"this.virtualMachine.name()"})
    private void restart() {
        virtualMachine.restart();
        refreshItems();
    }

    @AzureOperation(name = "user/vm.stop_vm.vm", params = {"this.virtualMachine.name()"})
    private void stop() {
        virtualMachine.stop();
        refreshItems();
    }

    @AzureOperation(name = "user/vm.open_portal.vm", params = {"this.virtualMachine.name()"})
    private void openInPortal() {
        this.openResourcesInPortal(ResourceId.fromString(this.virtualMachine.id()).subscriptionId(), this.virtualMachine.id());
    }

    private static final String WAIT_ICON_PATH = "VirtualMachineUpdating_16.png";
    private static final String STOP_ICON_PATH = "VirtualMachineStopped_16.png";
    private static final String RUN_ICON_PATH = "VirtualMachineRunning_16.png";

    private final VirtualMachine virtualMachine;
    private final String subscriptionId;

    public VMNode(Node parent, String subscriptionId, VirtualMachine virtualMachine) {
        super(virtualMachine.id(), virtualMachine.name(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;
        this.subscriptionId = subscriptionId;
        loadActions();

        // update vm icon based on vm status
        refreshItemsInternal();
    }

    private String getVMIconPath() {
        return RUN_ICON_PATH;
    }

    @Override
    protected void refreshItems() {
        virtualMachine.refresh();

        refreshItemsInternal();
    }

    private void refreshItemsInternal() {
        // update vm name and status icon
        setName(virtualMachine.name());
        setIconPath(getVMIconPath());
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        super.loadActions();
    }

    protected final BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(VMArmModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        final boolean started = isRunning();
        getNodeActionByName(AzureActionEnum.STOP.getName()).setEnabled(started);
        getNodeActionByName(AzureActionEnum.START.getName()).setEnabled(!started);
        getNodeActionByName(AzureActionEnum.RESTART.getName()).setEnabled(started);

        return super.getNodeActions();
    }

    private boolean isRunning() {
        return virtualMachine.getFormalStatus().isRunning();
    }
}
