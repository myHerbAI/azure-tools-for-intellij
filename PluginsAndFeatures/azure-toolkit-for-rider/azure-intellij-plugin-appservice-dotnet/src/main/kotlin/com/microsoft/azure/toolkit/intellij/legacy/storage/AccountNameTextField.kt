/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.storage

import com.azure.core.management.exception.ManagementException
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import kotlin.jvm.java
import kotlin.text.equals

class AccountNameTextField(private val subscriptionId: String) : AzureTextInput() {
    companion object {
        private const val pattern = "[a-z0-9]{3,24}"
        private val regex = Regex(pattern)
        private const val minLength = 3
        private const val maxLength = 24
    }

    init {
        isRequired = true
        setValidator(::doValidateValue)
    }

    private fun doValidateValue(): AzureValidationInfo {
        val currentValue = value

        if (currentValue.length < minLength || currentValue.length > maxLength) {
            return AzureValidationInfo
                .builder()
                .input(this)
                .message("Server name must be at least $minLength characters and at most $maxLength characters.")
                .type(AzureValidationInfo.Type.ERROR)
                .build()
        }

        if (!regex.matches(currentValue)) {
            return AzureValidationInfo
                .builder()
                .input(this)
                .message("The field can contain only lowercase letters and numbers. Name must be between 3 and 24 characters.")
                .type(AzureValidationInfo.Type.ERROR)
                .build()
        }

        try {
            val resultEntity = Azure.az(AzureStorageAccount::class.java)
                .forSubscription(subscriptionId)
                .checkNameAvailability(currentValue)
            if (!resultEntity.isAvailable) {
                var message = resultEntity.unavailabilityMessage
                if (message.equals("AlreadyExists", true)) message =
                    "The specified storage account name is already taken."
                return AzureValidationInfo
                    .builder()
                    .input(this)
                    .message(message)
                    .type(AzureValidationInfo.Type.ERROR)
                    .build()
            }
        } catch (e: ManagementException) {
            return AzureValidationInfo
                .builder()
                .input(this)
                .message(e.message)
                .type(AzureValidationInfo.Type.ERROR)
                .build()
        }

        return AzureValidationInfo.success(this)
    }
}