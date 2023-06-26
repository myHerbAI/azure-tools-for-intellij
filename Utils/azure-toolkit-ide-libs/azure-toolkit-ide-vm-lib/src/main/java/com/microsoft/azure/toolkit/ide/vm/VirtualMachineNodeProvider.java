/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.vm;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.compute.AzureCompute;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.model.OperatingSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VirtualMachineNodeProvider implements IExplorerNodeProvider {
    public static final AzureIconProvider<VirtualMachine> VIRTUAL_MACHINE_ICON_PROVIDER =
            new AzureResourceIconProvider<VirtualMachine>().withModifier(VirtualMachineNodeProvider::getOperatingSystemModifier);

    private static final String NAME = "Virtual Machines";
    private static final String ICON = AzureIcons.VirtualMachine.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureCompute.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureCompute ||
            data instanceof VirtualMachine;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureCompute) {
            return new AzServiceNode<>((AzureCompute) data)
                .withIcon(ICON).withLabel(NAME)
                .withActions(VirtualMachineActionsContributor.SERVICE_ACTIONS)
                .addChildren(AzureCompute::virtualMachines, (vm, vmNode) -> this.createNode(vm, vmNode, manager));
        } else if (data instanceof VirtualMachine) {
            return new AzResourceNode<>((VirtualMachine) data)
                .withIcon(VIRTUAL_MACHINE_ICON_PROVIDER::getIcon)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                .withActions(VirtualMachineActionsContributor.VM_ACTIONS);
        }
        return null;
    }


    @Nullable
    private static AzureIcon.Modifier getOperatingSystemModifier(VirtualMachine resource) {
        return resource.getOperatingSystem() != OperatingSystem.Windows ? AzureIcon.Modifier.LINUX : null;
    }
}
