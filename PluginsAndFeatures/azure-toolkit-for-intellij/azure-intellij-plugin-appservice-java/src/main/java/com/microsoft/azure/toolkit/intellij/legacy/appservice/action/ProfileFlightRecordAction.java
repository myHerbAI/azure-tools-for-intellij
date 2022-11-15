/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.action;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.PlatformUtils;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.jfr.RunFlightRecorderDialog;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.appservice.jfr.FlightRecorderConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.appservice.jfr.FlightRecorderManager;
import com.microsoft.azure.toolkit.lib.legacy.appservice.jfr.FlightRecorderStarterBase;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ProfileFlightRecordAction {
    private static final Logger logger = Logger.getLogger(ProfileFlightRecordAction.class.getName());
    private static final int ONE_SECOND = 1000;
    private static final int TWO_SECONDS = 2000;
    private final Project project;
    private final String subscriptionId;
    private final AppServiceAppBase<?, ?, ?> appService;

    public ProfileFlightRecordAction(@Nonnull final AppServiceAppBase<?, ?, ?> appService, @Nullable final Project project) {
        super();
        this.project = project;
        this.appService = appService;
        this.subscriptionId = appService.getSubscriptionId();
    }

    public void execute() {
        this.doProfileFlightRecorderAll();
    }

    private void doProfileFlightRecorderAll() {
        try {
            // Always get latest app service status, workaround for https://dev.azure.com/mseng/VSJava/_workitems/edit/1797916
            if (appService.getRuntime().getOperatingSystem() == OperatingSystem.DOCKER) {
                final String message = message("webapp.flightRecord.error.notSupport.message", appService.name());
                throw new AzureToolkitRuntimeException(message("webapp.flightRecord.error.notSupport.title"), message);
            }
            if (!StringUtils.equalsIgnoreCase(appService.getStatus(), "running")) {
                final String message = message("webapp.flightRecord.error.notRunning.message", appService.name());
                throw new AzureToolkitRuntimeException(message("webapp.flightRecord.error.notRunning.title"), message);
            }
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setText(message("webapp.flightRecord.task.startProfileWebApp.title", appService.name()));
            final CountDownLatch finishLatch = new CountDownLatch(1);
            AzureTaskManager.getInstance().runAndWait(() -> {
                final FlightRecorderConfiguration config = collectFlightRecorderConfiguration();
                if (Objects.isNull(config)) {
                    AzureMessager.getMessager().warning(message("webapp.flightRecord.error.cancelled.message"), message("webapp.flightRecord.error.cancelled.title"));
                    finishLatch.countDown();
                    return;
                }
                if (config.getDuration() <= 0) {
                    finishLatch.countDown();
                    throw new AzureToolkitRuntimeException(message("webapp.flightRecord.error.invalidDuration.title"), message("webapp.flightRecord.error.invalidDuration.message"));
                } else {
                    new Thread(() -> doProfileFlightRecorder(progressIndicator, config, finishLatch)).start();
                }
            }, AzureTask.Modality.NONE);
            finishLatch.await();
        } catch (final Exception ex) {
            throw new AzureToolkitRuntimeException(message("webapp.flightRecord.error.profileFailed.title"), message("webapp.flightRecord.error.profileFailed.message") + ex.getMessage());
        }
    }

    private FlightRecorderConfiguration collectFlightRecorderConfiguration() {
        final RunFlightRecorderDialog ui = new RunFlightRecorderDialog(project, appService);
        ui.setTitle(message("webapp.flightRecord.task.startRecorder.title", appService.getHostName()));
        ui.setOkActionListener((config) -> ui.close(DialogWrapper.OK_EXIT_CODE));

        if (ui.showAndGet()) {
            return ui.getValue();
        }
        return null;
    }

    @AzureOperation(name = "appservice.profile_flight_recorder_in_azure", type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private void doProfileFlightRecorder(ProgressIndicator progressIndicator, FlightRecorderConfiguration config, CountDownLatch latch) {
        try {
            progressIndicator.setText(String.format(message("webapp.flightRecord.task.startProcessRecorder.title"), config.getPid(), config.getProcessName()));
            final File file = File.createTempFile("jfr-snapshot-" + appService.name() + "-", ".jfr");
            FileUtils.forceDeleteOnExit(file);
            final FlightRecorderStarterBase starter = FlightRecorderManager.getFlightRecorderStarter(appService);
            starter.startFlightRecorder(config.getPid(), config.getDuration(), file.getName());
            progressIndicator.setText(message("webapp.flightRecord.hint.recording", config.getDuration()));

            progressIndicator.checkCanceled();
            try {
                Thread.sleep(ONE_SECOND * config.getDuration() + TWO_SECONDS);
            } catch (final InterruptedException e) {
                // ignore
            }
            progressIndicator.checkCanceled();
            progressIndicator.setText(message("webapp.flightRecord.hint.profileCompletedOnAzure"));
            progressIndicator.setText(message("webapp.flightRecord.hint.downloadingJfr"));
            final byte[] content = starter.downloadJFRFile(file.getName());
            if (content != null) {
                FileUtils.writeByteArrayToFile(file, content);
                progressIndicator.setText(message("webapp.flightRecord.hint.downloadingJfrDone"));
                AzureMessager.getMessager().info(getActionOnJfrFile(file.getAbsolutePath()), message("webapp.flightRecord.hint.profileRecorderComplete"));
            } else {
                throw new AzureToolkitRuntimeException(message("webapp.flightRecord.error.jfrDownload.title"), message("webapp.flightRecord.error.jfrDownload.message"));
            }

        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(message("webapp.flightRecord.error.profileFlightRecorderFailed.title"), e);
        } finally {
            latch.countDown();
        }
    }

    private String getActionOnJfrFile(String filePath) {
        if (PlatformUtils.isIdeaUltimate()) {
            return String.format(message("webapp.flightRecord.hint.openJfrIntelliJ"), filePath);
        } else {
            return message("webapp.flightRecord.hint.openJfrZuluMissionControl", filePath);
        }
    }

}
