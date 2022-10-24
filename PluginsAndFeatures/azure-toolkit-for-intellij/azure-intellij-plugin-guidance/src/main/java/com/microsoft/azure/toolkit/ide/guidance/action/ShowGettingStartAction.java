package com.microsoft.azure.toolkit.ide.guidance.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.AzureAnAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.*;

public class ShowGettingStartAction extends AzureAnAction {
    public static final String GUIDANCE = "guidance";
    public static final String IS_ACTION_TRIGGERED = "isActionTriggered";
    private static boolean isActionTriggered = false;

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        if (anActionEvent.getProject() != null) {
            if (!isActionTriggered) {
                isActionTriggered = true;
                AzureStoreManager.getInstance().getIdeStore().setProperty(GUIDANCE, IS_ACTION_TRIGGERED, String.valueOf(true));
            }
            GuidanceViewManager.getInstance().showCoursesView(anActionEvent.getProject());
        }
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        if (!isActionTriggered) {
            final String isActionTriggerVal = AzureStoreManager.getInstance().getIdeStore().getProperty(GUIDANCE, IS_ACTION_TRIGGERED);
            isActionTriggered = Optional.ofNullable(isActionTriggerVal).map(Boolean::getBoolean).orElse(false);
        }
        e.getPresentation().setIcon(IntelliJAzureIcons.getIcon(isActionTriggered ? GET_START_NEW : GET_START));
    }

}
