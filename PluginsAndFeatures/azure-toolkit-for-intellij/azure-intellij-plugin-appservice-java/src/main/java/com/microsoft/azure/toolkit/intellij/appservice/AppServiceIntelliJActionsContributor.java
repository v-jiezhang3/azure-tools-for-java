/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.appservice.function.coretools.FunctionsCoreToolsManager;
import com.microsoft.azure.toolkit.ide.appservice.webapp.WebAppActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.appservice.actions.AppServiceFileAction;
import com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction;
import com.microsoft.azure.toolkit.intellij.function.remotedebug.FunctionEnableRemoteDebuggingAction;
import com.microsoft.azure.toolkit.intellij.function.remotedebug.FunctionRemoteDebuggingAction;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.action.*;
import com.microsoft.azure.toolkit.intellij.legacy.docker.action.PushToContainerRegistryAction;
import com.microsoft.azure.toolkit.intellij.legacy.function.action.CreateFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.legacy.function.action.DeployFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.CreateWebAppAction;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.DeployWebAppAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileActionsContributor.APP_SERVICE_FILE_DOWNLOAD;
import static com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileActionsContributor.APP_SERVICE_FILE_VIEW;

public class AppServiceIntelliJActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER =
        Math.max(AppServiceActionsContributor.INITIALIZE_ORDER, ContainerRegistryActionsContributor.INITIALIZE_ORDER) + 1;
    private static final String UNABLE_TO_OPEN_EXPLORER = "Unable to open explorer";

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureTaskManager tm = AzureTaskManager.getInstance();

        new Action<>(APP_SERVICE_FILE_VIEW)
            .withLabel("Open File")
            .withIdParam(AppServiceFile::getName)
            .visibleWhen(s -> s instanceof AppServiceFile)
            .withHandler((file, e) -> tm.runLater(() -> new AppServiceFileAction().openAppServiceFile(file, ((AnActionEvent) e).getProject())))
            .withShortcut(am.getIDEDefaultShortcuts().edit())
            .register(am);

        new Action<>(APP_SERVICE_FILE_DOWNLOAD)
            .withLabel("Download")
            .withIdParam(AppServiceFile::getName)
            .visibleWhen(s -> s instanceof AppServiceFile)
            .withHandler((file, e) -> tm.runLater(() -> new AppServiceFileAction().saveAppServiceFile(file, ((AnActionEvent) e).getProject(), null)))
            .register(am);
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AppServiceAppBase<?, ?, ?>, AnActionEvent> isAppService = (r, e) -> r instanceof AppServiceAppBase<?, ?, ?>;
        final BiPredicate<AppServiceAppBase<?, ?, ?>, AnActionEvent> nonLinuxFunction = (r, e) -> Objects.nonNull(r) &&
            !(r instanceof FunctionApp && Optional.ofNullable(r.getRuntime()).map(Runtime::isLinux).orElse(Boolean.FALSE));
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        final BiConsumer<AppServiceAppBase<?, ?, ?>, AnActionEvent> flightRecorderHandler = (c, e) ->
            tm.runLater(() -> new ProfileFlightRecordAction(c, e.getProject()).execute());
        am.registerHandler(AppServiceActionsContributor.PROFILE_FLIGHT_RECORD, isAppService, flightRecorderHandler);

        final BiConsumer<AppServiceAppBase<?, ?, ?>, AnActionEvent> startStreamingLogHandler = (c, e) ->
            new StartStreamingLogsAction(c, e.getProject()).execute();
        am.registerHandler(AppServiceActionsContributor.START_STREAM_LOG, isAppService, startStreamingLogHandler);

        final BiConsumer<AppServiceAppBase<?, ?, ?>, AnActionEvent> stopStreamingLogHandler = (c, e) ->
            tm.runLater(() -> new StopStreamingLogsAction(c, e.getProject()).execute());
        am.registerHandler(AppServiceActionsContributor.STOP_STREAM_LOG, nonLinuxFunction, stopStreamingLogHandler);

        final BiConsumer<AppServiceAppBase<?, ?, ?>, AnActionEvent> openLogsInMonitorHandler = (c, e) ->
                new OpenLogsInMonitorAction(c, e.getProject()).execute();
        am.registerHandler(AppServiceActionsContributor.OPEN_LOGS_IN_MONITOR, isAppService, openLogsInMonitorHandler);

        final BiPredicate<AppServiceAppBase<?, ?, ?>, AnActionEvent> isWebApp = (r, e) -> r instanceof WebApp;
        final BiConsumer<AppServiceAppBase<?, ?, ?>, AnActionEvent> sshHandler = (c, e) ->
            tm.runLater(() -> new SSHIntoWebAppAction((WebApp) c, e.getProject()).execute());
        am.registerHandler(AppServiceActionsContributor.SSH_INTO_WEBAPP, isAppService, sshHandler);

        final BiConsumer<AzResource, AnActionEvent> deployWebAppHandler = (c, e) -> DeployWebAppAction.deploy((WebApp) c, Objects.requireNonNull(e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, (r, e) -> r instanceof WebApp, deployWebAppHandler);

        final BiConsumer<Object, AnActionEvent> createWebAppHandler = (c, e) -> CreateWebAppAction.openDialog(e.getProject(), null);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof AzureWebApp, createWebAppHandler);

        final BiConsumer<AzResource, AnActionEvent> deployFunctionAppHandler = (c, e) -> DeployFunctionAppAction.deploy((FunctionApp) c, Objects.requireNonNull(e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, (r, e) -> r instanceof FunctionApp, deployFunctionAppHandler);

        final BiConsumer<Object, AnActionEvent> createFunctionHandler = (c, e) -> CreateFunctionAppAction.openDialog(e.getProject(), null);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof AzureFunctions, createFunctionHandler);

        final BiConsumer<AzResourceBase, AnActionEvent> showFunctionPropertyViewHandler = (c, e) -> tm
            .runLater(() -> new OpenAppServicePropertyViewAction().openFunctionAppPropertyView((FunctionApp) c, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, (r, e) -> r instanceof FunctionApp, showFunctionPropertyViewHandler);

        final BiConsumer<AzResourceBase, AnActionEvent> showFunctionSlotPropertyViewHandler = (c, e) -> tm
            .runLater(() -> new OpenAppServicePropertyViewAction().openFunctionAppDeploymentSlotPropertyView((FunctionAppDeploymentSlot) c, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, (r, e) -> r instanceof FunctionAppDeploymentSlot, showFunctionSlotPropertyViewHandler);

        final BiConsumer<AzResourceBase, AnActionEvent> showWebAppPropertyViewHandler = (c, e) -> tm
            .runLater(() -> new OpenAppServicePropertyViewAction().openWebAppPropertyView((WebApp) c, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, (r, e) -> r instanceof WebApp, showWebAppPropertyViewHandler);

        final BiConsumer<AzResourceBase, AnActionEvent> showWebAppSlotPropertyViewHandler = (c, e) -> tm
            .runLater(() -> new OpenAppServicePropertyViewAction().openDeploymentSlotPropertyView((WebAppDeploymentSlot) c, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, (r, e) -> r instanceof WebAppDeploymentSlot, showWebAppSlotPropertyViewHandler);

        final BiPredicate<FunctionEntity, AnActionEvent> triggerPredicate = (r, e) -> r instanceof FunctionEntity;
        final BiConsumer<FunctionEntity, AnActionEvent> triggerFunctionHandler = (entity, e) -> {
            final String functionId = Optional.ofNullable(entity.getFunctionAppId())
                .orElseGet(() -> ResourceId.fromString(entity.getTriggerId()).parent().id());
            final FunctionApp functionApp = Azure.az(AzureFunctions.class).functionApp(functionId);
            final String triggerType = Optional.ofNullable(entity.getTrigger())
                .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
            final Object request;
            if (StringUtils.equalsIgnoreCase(triggerType, "timertrigger")) {
                request = new Object();
            } else {
                final String input = tm.runAndWaitAsObservable(new AzureTask<>(() -> Messages.showInputDialog(e.getProject(), "Please set the input value: ",
                    String.format("Trigger function %s", entity.getName()), null))).toBlocking().single();
                if (input == null) {
                    return;
                }
                request = new TriggerRequest(input);
            }
            functionApp.triggerFunction(entity.getName(), request);
        };
        am.registerHandler(FunctionAppActionsContributor.TRIGGER_FUNCTION, triggerPredicate, triggerFunctionHandler);

        // keep push docker image in app service library as form is shared between appservice/container repository but could not split into different project
        final BiPredicate<ContainerRegistry, AnActionEvent> pushImageCondition = (r, e) -> r instanceof ContainerRegistry;
        final BiConsumer<ContainerRegistry, AnActionEvent> pushImageHandler =
            (c, e) -> PushToContainerRegistryAction.execute(c, e.getProject());
        am.registerHandler(ContainerRegistryActionsContributor.PUSH_IMAGE, pushImageCondition, pushImageHandler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateFunctionHandler =
            (r, e) -> CreateFunctionAppAction.openDialog(e.getProject(),
                FunctionAppConfig.getFunctionAppDefaultConfig().toBuilder()
                    .subscription(r.getSubscription())
                    .region(r.getRegion())
                    .resourceGroup(ResourceGroupConfig.fromResource(r)).build());
        am.registerHandler(FunctionAppActionsContributor.GROUP_CREATE_FUNCTION, (r, e) -> true, groupCreateFunctionHandler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateWebAppHandler =
            (r, e) -> CreateWebAppAction.openDialog(e.getProject(),
                WebAppConfig.getWebAppDefaultConfig().toBuilder()
                    .subscription(r.getSubscription())
                    .region(r.getRegion())
                    .resourceGroup(ResourceGroupConfig.fromResource(r)).build());
        am.registerHandler(WebAppActionsContributor.GROUP_CREATE_WEBAPP, (r, e) -> true, groupCreateWebAppHandler);

        final BiPredicate<FunctionAppBase<?, ?, ?>, AnActionEvent> isFunction = (r, e) -> r instanceof FunctionAppBase<?, ?, ?>;
        final BiConsumer<FunctionAppBase<?, ?, ?>, AnActionEvent> enableRemoteDebuggingHandler = (c, e) ->
            FunctionEnableRemoteDebuggingAction.enableRemoteDebugging(c, e.getProject());
        am.registerHandler(FunctionAppActionsContributor.ENABLE_REMOTE_DEBUGGING, isFunction, enableRemoteDebuggingHandler);

        final BiConsumer<FunctionAppBase<?, ?, ?>, AnActionEvent> disableRemoteDebuggingHandler = (c, e) ->
            FunctionEnableRemoteDebuggingAction.disableRemoteDebugging(c, e.getProject());
        am.registerHandler(FunctionAppActionsContributor.DISABLE_REMOTE_DEBUGGING, isFunction, disableRemoteDebuggingHandler);

        final BiConsumer<FunctionAppBase<?, ?, ?>, AnActionEvent> remoteDebuggingHandler = (c, e) ->
            tm.runLater(() -> FunctionRemoteDebuggingAction.startDebugging(c, e.getProject()));
        am.registerHandler(FunctionAppActionsContributor.REMOTE_DEBUGGING, isFunction, remoteDebuggingHandler);

        final BiConsumer<Object, AnActionEvent> downloadFuncCoreToolsHandler = (v, e) -> {
            final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            descriptor.setTitle("Select Path to Install Azure Functions Core Tools");
            tm.runLater(() -> FileChooser.chooseFile(descriptor, null, null, files -> {
                final String installPath = files.getPath();
                tm.runInBackground("Download and Install Azure Functions Core Tools",
                    () -> FunctionsCoreToolsManager.getInstance().downloadReleaseTools(installPath));
            }));
        };
        am.registerHandler(FunctionAppActionsContributor.DOWNLOAD_CORE_TOOLS, downloadFuncCoreToolsHandler);
        AzureEventBus.on("function.download_func_core_tools_succeed.version", new AzureEventBus.EventListener((azureEvent) -> {
            final Action<Object> openSettingsActionInMessage = am.getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
            final String INSTALL_SUCCEED_MESSAGE = "Download and install Azure Functions Core Tools successfully. Auto configured Azure Functions Core Tools path in Azure Settings";
            AzureMessager.getMessager().success(INSTALL_SUCCEED_MESSAGE, null, openSettingsActionInMessage);
        }));
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }

    @RequiredArgsConstructor
    static class TriggerRequest {
        private final String input;
    }
}
