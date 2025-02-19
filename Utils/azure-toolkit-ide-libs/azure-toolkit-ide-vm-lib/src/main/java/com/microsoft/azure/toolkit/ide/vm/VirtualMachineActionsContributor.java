/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.vm;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class VirtualMachineActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.vm.service";
    public static final String VM_ACTIONS = "actions.vm.management";

    public static final Action.Id<VirtualMachine> ADD_SSH_CONFIG = Action.Id.of("user/vm.add_ssh_config.vm");
    public static final Action.Id<VirtualMachine> CONNECT_SSH = Action.Id.of("user/vm.connect_using_ssh.vm");
    public static final Action.Id<VirtualMachine> SFTP_CONNECTION = Action.Id.of("user/vm.browse_files_sftp.vm");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_VM = Action.Id.of("user/vm.create_vm.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(ADD_SSH_CONFIG)
            .withLabel("Edit SSH Configuration")
            .withIcon(AzureIcons.Action.ADD.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof VirtualMachine)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);

        new Action<>(CONNECT_SSH)
            .withLabel("Connect Using SSH")
            .withIcon(AzureIcons.Action.CONSOLE.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof VirtualMachine)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);

        new Action<>(SFTP_CONNECTION)
            .withLabel("Browse Files Using SFTP")
            .withIcon(AzureIcons.Action.SFTP.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof VirtualMachine)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .register(am);

        new Action<>(GROUP_CREATE_VM)
            .withLabel("Virtual Machine")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus(true).isConnected())
            .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            VirtualMachineActionsContributor.ADD_SSH_CONFIG,
            VirtualMachineActionsContributor.CONNECT_SSH,
            VirtualMachineActionsContributor.SFTP_CONNECTION,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(VM_ACTIONS, accountActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_VM);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
