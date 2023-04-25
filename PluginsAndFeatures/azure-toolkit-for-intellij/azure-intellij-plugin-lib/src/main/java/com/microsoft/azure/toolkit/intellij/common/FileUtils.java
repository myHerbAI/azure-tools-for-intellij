/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    @Nullable
    public static Path getNearestExistingParent(Path path) {
        // Check if the path exists
        if (Files.exists(path)) {
            // Return the path itself if it exists
            return path;
        } else {
            // Get the parent path of the given path
            final Path parent = path.getParent();
            // If the parent is null, return null
            if (parent == null) {
                return null;
            } else {
                // Recursively call this method on the parent until an existing path is found or null is returned
                return getNearestExistingParent(parent);
            }
        }
    }
}
