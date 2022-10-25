/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.storage.component;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.storage.blob.IBlobFile;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;

public class FileNameTextField extends AzureTextInput {

    private static final String[] invalidBlobChars = new String[]{"\"", "\\", ":", "|", "<", ">", "?", "*"};
    private static final String invalidBlobCharsString = StringUtils.join(invalidBlobChars, ", ");
    private static final String INVALID_BLOB_CHARACTERS = "Avoid blob names that contain the following characters: " + invalidBlobCharsString;
    private static final String INVALID_BLOB_LENGTH = "Avoid blob names that exceed 255 characters.";
    private static final String CONFLICT_BLOB_NAME = "A blob with the same name already exists.";

    private static final String invalidBlobDirectoryChar = "\\"; // Keeps behavior consistent on Mac & Windows when creating blob directories via the file explorer
    private static final String[] invalidFileAndDirectoryChars = new String[]{"\"", "/", "\\", ":", "|", "<", ">", "?", "*"};
    private static final String invalidFileAndDirectoryCharsString = StringUtils.join(invalidFileAndDirectoryChars, ", ");
    private static final String INVALID_CHARACTERS = "Name cannot contain the following characters: " + invalidFileAndDirectoryCharsString;
    private static final String INVALID_LENGTH = "Name must contain between 1 and 255 characters.";
    private static final String CONFLICT_NAME = "A file/directory with the same name already exists.";
    private StorageFile parent;

    public FileNameTextField() {
        super();
        this.addValidator(this::doValidateValue);
        this.setRequired(true);
    }

    public void setParent(StorageFile parent) {
        this.parent = parent;
        // UI dialog will call set subscription after shown, which may trigger validation and show unnecessary error message
        // so skip validation if validation info is not set, we just need to revalidate after change to another subscription
        if (this.getValidationInfo() != null) {
            this.validateValueAsync();
        }
    }

    public AzureValidationInfo doValidateValue() {
        if (this.parent instanceof IBlobFile) {
            return this.doValidateBlobName();
        }
        final String value = this.getValue();
        if (StringUtils.length(value) < 1) {
            return AzureValidationInfo.error("The value must not be empty.", this);
        } else if (StringUtils.length(value) > 255) {
            return AzureValidationInfo.error(INVALID_LENGTH, this);
        } else if (StringUtils.containsAny(value, invalidFileAndDirectoryChars)) {
            return AzureValidationInfo.error(INVALID_CHARACTERS, this);
        } else if (this.parent.getSubFileModule().exists(value, this.parent.getResourceGroupName())) {
            return AzureValidationInfo.error(CONFLICT_NAME, this);
        }
        return AzureValidationInfo.success(this);
    }

    public AzureValidationInfo doValidateBlobName() {
        final String value = this.getValue();
        final IBlobFile blob = (IBlobFile) this.parent;
        if (StringUtils.length(value) < 1) {
            return AzureValidationInfo.error("The value must not be empty.", this);
        } else if (StringUtils.length(value) > 255) {
            return AzureValidationInfo.error(INVALID_BLOB_LENGTH, this);
        } else if (StringUtils.containsAny(value, invalidBlobChars)) {
            return AzureValidationInfo.error(INVALID_BLOB_CHARACTERS, this);
        } else if (blob.getContainer().exists(Paths.get(this.parent.getPath(), value).toString())) {
            return AzureValidationInfo.error(CONFLICT_BLOB_NAME, this);
        }
        return AzureValidationInfo.success(this);
    }
}
