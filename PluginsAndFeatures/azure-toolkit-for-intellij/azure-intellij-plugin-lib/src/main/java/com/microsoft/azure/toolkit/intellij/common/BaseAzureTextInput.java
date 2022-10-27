/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.google.common.collect.ImmutableMap;
import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class BaseAzureTextInput<T> extends ExtendableTextField
    implements AzureFormInputComponent<T>, TextDocumentListenerAdapter {
    protected static final int DEBOUNCE_DELAY = 500;
    private final Debouncer debouncer;
    private static final Extension VALIDATING = Extension.create(AnimatedIcon.Default.INSTANCE, "Validating...", null);
    private static final Extension SUCCESS = Extension.create(AllIcons.General.InspectionsOK, "Validation passed.", null);
    private static final Map<AzureValidationInfo.Type, Function<AzureValidationInfo, Extension>> extensions = ImmutableMap.of(
        AzureValidationInfo.Type.PENDING, (i) -> VALIDATING,
        AzureValidationInfo.Type.SUCCESS, (i) -> SUCCESS,
        AzureValidationInfo.Type.ERROR, (i) -> Extension.create(AllIcons.General.BalloonError, i.getMessage(), null),
        AzureValidationInfo.Type.WARNING, (i) -> Extension.create(AllIcons.General.BalloonWarning, i.getMessage(), null)
    );
    protected Extension validationExtension;

    public BaseAzureTextInput() {
        super();
        this.debouncer = new TailingDebouncer(this::fireValueChangedEvent, DEBOUNCE_DELAY);
        this.getInputComponent().getDocument().addDocumentListener(this);
        this.trackValidation();
    }

    public BaseAzureTextInput(@Nonnull JTextField comp) {
        super();
        this.debouncer = new TailingDebouncer(this::fireValueChangedEvent, DEBOUNCE_DELAY);
        comp.getDocument().addDocumentListener(this);
        this.trackValidation();
    }

    public void setValidationInfo(@Nullable AzureValidationInfo info) {
        AzureFormInputComponent.super.setValidationInfo(info);
        final Extension ex = Objects.isNull(info) ? null : extensions.get(info.getType()).apply(info);
        this.setValidationExtension(ex);
    }

    public void onDocumentChanged() {
        if (this.needValidation()) {
            this.setValidationInfo(AzureValidationInfo.pending(this));
        }
        this.debouncer.debounce();
    }

    protected synchronized void setValidationExtension(@Nullable final Extension extension) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (validationExtension != null) {
                this.removeExtension(validationExtension);
            }
            if (Objects.nonNull(extension)) {
                this.addExtension(extension);
            }
            this.validationExtension = extension;
        }, AzureTask.Modality.ANY);
    }

    @Override
    public JTextField getInputComponent() {
        return this;
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", this.getClass().getSimpleName(), this.getLabel());
    }
}
