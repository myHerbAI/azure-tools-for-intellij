/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.experiment;

import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ExperimentationClient {
    private static final String ASSIGNMENT_UNIT_ID = "clientId";
    private static final String AUDIENCE_FILTER_ID = "userstype";
    private static final String AUDIENCE_FILTER_VALUE = "intellij";
    private static final String END_POINT = "https://aka.ms/azure-ij-ab-exp";
    private static ExperimentationService experimentationService;
    private static boolean isInited = false;

    public static void init() {
        if (isInited) {
            return;
        }
        isInited = true;
        final Map<String, String> audienceFilters = new HashMap<>();
        final Map<String, String> assignmentIds = new HashMap<>();
        audienceFilters.put(ASSIGNMENT_UNIT_ID, InstallationIdUtils.getHashMac());
        experimentationService = new ExperimentationService()
                .withEndPoint(END_POINT)
                .withAudienceFilters(audienceFilters)
                .withAssignmentIds(assignmentIds)
                .create();
    }

    @Nullable
    public static String getFeatureVariable(String featureFlagName) {
        if (!isInited) {
            init();
        }
        return experimentationService.getFeatureVariable(featureFlagName);
    }

    @Nullable
    public static String getAssignmentContext() {
        if (!isInited) {
            init();
        }
        return experimentationService.getAssignmentContext();
    }

    public enum FeatureFlag {
        GETTING_STARTED_UI("showNewUI");
        @Getter
        private final String flagName;

        FeatureFlag(String flagName) {
            this.flagName = flagName;
        }
    }

}
