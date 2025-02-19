/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.creation.SpringCloudAppCreationDialog;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudAppComboBox extends AzureComboBox<SpringCloudApp> {
    private SpringCloudCluster cluster;
    private final List<SpringCloudApp> draftItems = new LinkedList<>();
    @Setter
    @Nullable
    private Integer javaVersion;

    public SpringCloudAppComboBox() {
        super();
        this.setRenderer(new AppItemRenderer());
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final SpringCloudApp app = (SpringCloudApp) item;
        final String runtime = Optional.ofNullable(app.getCachedActiveDeployment()).map(SpringCloudDeployment::getRuntimeVersion)
            .map(v -> v.replaceAll("_", " ")).orElse(null);
        final String appName = app.exists() ? app.getName() : String.format("(New) %s", app.getName());
        return StringUtils.isBlank(runtime) ? appName : String.format("%s (%s)", appName, runtime);
    }

    public void setCluster(SpringCloudCluster cluster) {
        if (Objects.equals(cluster, this.cluster)) {
            return;
        }
        this.cluster = cluster;
        if (cluster == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable SpringCloudApp val) {
        if (Objects.nonNull(val) && val.isDraftForCreating()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val);
    }

    @Nullable
    @Override
    protected SpringCloudApp doGetDefaultValue() {
        return CacheManager.getUsageHistory(SpringCloudApp.class)
            .peek(v -> Objects.isNull(cluster) || Objects.equals(cluster, v.getParent()));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/springcloud.list_apps.cluster", params = {"this.cluster.name()"})
    protected List<? extends SpringCloudApp> loadItems() {
        final List<SpringCloudApp> apps = new ArrayList<>();
        if (Objects.nonNull(this.cluster)) {
            if (!this.draftItems.isEmpty()) {
                apps.addAll(this.draftItems.stream().filter(a -> a.getParent().getName().equals(this.cluster.getName())).toList());
            }
            apps.addAll(cluster.apps().list());
        }
        return apps;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.cluster).ifPresent(c -> c.apps().refresh());
        super.refreshItems();
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create Azure Spring App (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::showAppCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void showAppCreationPopup() {
        final SpringCloudAppCreationDialog dialog = new SpringCloudAppCreationDialog(this.cluster);
        Optional.ofNullable(this.javaVersion).ifPresent(a -> dialog.setDefaultRuntimeVersion(javaVersion));
        dialog.setOkActionListener((config) -> {
            final SpringCloudAppDraft app = cluster.apps().create(config.getAppName(), cluster.getResourceGroupName());
            app.setConfig(config);
            dialog.close();
            this.setValue(app);
        });
        dialog.show();
    }

    public static class AppItemRenderer extends ColoredListCellRenderer<SpringCloudApp> {
        @Override
        protected void customizeCellRenderer(@Nonnull JList<? extends SpringCloudApp> list, SpringCloudApp app, int index, boolean selected, boolean hasFocus) {
            if (app != null) {
                append(app.exists() ? app.getName() : String.format("(New) %s", app.getName()));
                if (app.getFormalStatus().isReading()) {
                    append(" Loading runtime...", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
                } else {
                    final String runtime = Optional.ofNullable(app.getCachedActiveDeployment())
                        .map(SpringCloudDeployment::getRuntimeVersion)
                        .map(v -> v.replaceAll("_", " ")).orElse("Unknown runtime");
                    append(" " + runtime, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
                }
            }
        }
    }
}
