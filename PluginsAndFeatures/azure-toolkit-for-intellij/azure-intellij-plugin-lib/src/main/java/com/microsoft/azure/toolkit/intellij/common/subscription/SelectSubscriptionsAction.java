/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.subscription;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SelectSubscriptionsAction extends AzureAnAction implements DumbAware {
    private static final Logger LOGGER = Logger.getInstance(SelectSubscriptionsAction.class);

    public SelectSubscriptionsAction() {
    }

    @Override
    @AzureOperation(name = "user/account.select_subscription")
    public boolean onActionPerformed(@NotNull AnActionEvent e, @Nullable Operation operation) {
        selectSubscriptions(e.getProject());
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        try {
            final boolean isSignIn = Azure.az(AzureAccount.class).isLoggedIn();
            e.getPresentation().setEnabled(isSignIn);
        } catch (final Exception ex) {
            ex.printStackTrace();
            LOGGER.error("update", ex);
        }
    }

    public static void selectSubscriptions(Project project) {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final SubscriptionsDialog dialog = new SubscriptionsDialog(project);
            dialog.select(selected -> manager.runOnPooledThread(() -> Azure.az(AzureAccount.class).account().setSelectedSubscriptions(selected)));
        });
    }
}
