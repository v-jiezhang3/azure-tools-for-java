/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.JBSplitter;
import com.microsoft.azure.toolkit.intellij.monitor.view.AzureMonitorView;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.WorkspaceSelectionDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import lombok.Getter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class MonitorSingleTab {
    @Getter
    private JBSplitter splitter;
    private MonitorLogTablePanel monitorLogTablePanel;
    private MonitorLogDetailsPanel monitorLogDetailsPanel;
    private final boolean isTableTab;
    private final String tabName;
    private final AzureMonitorView parentView;

    public MonitorSingleTab(boolean isTableTab, String tabName, AzureMonitorView parentView, String resourceId) {
        this.isTableTab = isTableTab;
        this.tabName = tabName;
        this.parentView = parentView;
        this.initUI();
        this.initListener();
        this.monitorLogTablePanel.setInitResourceId(resourceId);
        AzureTaskManager.getInstance().runInBackground(AzureString.fromString("Loading logs"), () -> {
            loadFilters(tabName);
            loadLogs();
        });
    }

    private void initUI() {
        this.splitter = new JBSplitter();
        this.monitorLogTablePanel = new MonitorLogTablePanel();
        this.monitorLogDetailsPanel = new MonitorLogDetailsPanel();
        this.splitter.setProportion(0.8f);
        this.splitter.setFirstComponent(this.monitorLogTablePanel.getContentPanel());
        this.splitter.setSecondComponent(this.monitorLogDetailsPanel.getContentPanel());
    }

    private void initListener() {
        this.monitorLogTablePanel.addTableSelectionListener(e -> {
            final String viewerTitle = monitorLogTablePanel.getSelectedColumnName();
            final String viewerText = monitorLogTablePanel.getSelectedCellValue();
            if (!e.getValueIsAdjusting() || Objects.isNull(viewerText) || Objects.isNull(viewerTitle)) {
                return;
            }
            this.monitorLogDetailsPanel.setViewText(viewerTitle, viewerText);
        });
        this.monitorLogTablePanel.addRunActionListener(new ActionListener() {
            @Override
            @AzureOperation(name = "user/monitor.execute_query")
            public void actionPerformed(ActionEvent e) {
                Optional.ofNullable(parentView.getSelectedWorkspace())
                        .ifPresentOrElse(t -> loadLogs(),
                        () -> AzureMessager.getMessager().info(message("azure.monitor.info.selectWorkspace"), null, selectWorkspaceAction()));
            }
        });
    }

    private void loadLogs() {
        final LogAnalyticsWorkspace selectedWorkspace = this.parentView.getSelectedWorkspace();
        final String queryString = this.isTableTab ? this.monitorLogTablePanel.getQueryStringFromFilters(tabName) : this.parentView.getQueryString(tabName);
        this.monitorLogTablePanel.loadTableModel(selectedWorkspace, queryString);
        this.monitorLogDetailsPanel.setStatus("No table cell is selected");
    }

    private void loadFilters(String tableName) {
        if (!this.isTableTab) {
            return;
        }
        final LogAnalyticsWorkspace selectedWorkspace = this.parentView.getSelectedWorkspace();
        this.monitorLogTablePanel.loadFilters(selectedWorkspace, tableName);
    }

    private Action<?> selectWorkspaceAction() {
        final Project project = ProjectManager.getInstance().getDefaultProject();
        return new Action<>(Action.Id.of("user/monitor.select_workspace"))
                .withLabel("Select")
                .withHandler((s, e) -> AzureTaskManager.getInstance().runLater(() -> {
                    final WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(project, null);
                    if (dialog.showAndGet()) {
                        Optional.ofNullable(dialog.getWorkspace()).ifPresent(w -> AzureEventBus.emit("azure.monitor.change_workspace", w));
                    }
                }));
    }

}
