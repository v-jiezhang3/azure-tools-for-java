/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.sqlserver;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SqlServerNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "SQL Server";
    private static final String ICON = AzureIcons.SqlServer.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureSqlServer.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureSqlServer || data instanceof MicrosoftSqlServer;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureSqlServer) {
            final AzureSqlServer service = ((AzureSqlServer) data);
            final Function<AzureSqlServer, List<MicrosoftSqlServer>> servers = s -> s.list().stream()
                .flatMap(m -> m.servers().list().stream()).collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(SqlServerActionsContributor.SERVICE_ACTIONS)
                .addChildren(servers, (server, serviceNode) -> this.createNode(server, serviceNode, manager));
        } else if (data instanceof MicrosoftSqlServer) {
            final MicrosoftSqlServer server = (MicrosoftSqlServer) data;
            return new Node<>(server)
                .view(new AzureResourceLabelView<>(server))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .doubleClickAction(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .actions(SqlServerActionsContributor.SERVER_ACTIONS);
        }
        return null;
    }
}
