/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
public class IntelliJWebAppSettingModel extends WebAppSettingModel {
    private String appSettingsKey = UUID.randomUUID().toString();
    private Map<String,String> appSettings;
    private Set<String> appSettingsToRemove;
    private AzureArtifactType azureArtifactType;
    private boolean openBrowserAfterDeployment = true;
    private boolean slotPanelVisible = false;
    private String artifactIdentifier;
    private String packaging;

}
