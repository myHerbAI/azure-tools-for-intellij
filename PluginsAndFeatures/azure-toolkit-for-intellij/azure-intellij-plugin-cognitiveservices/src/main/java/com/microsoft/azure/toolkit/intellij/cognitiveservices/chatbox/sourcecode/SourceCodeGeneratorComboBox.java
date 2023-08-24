package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;

import java.util.Arrays;

public class SourceCodeGeneratorComboBox extends AzureComboBox<ISourceCodeGenerator> {
    public SourceCodeGeneratorComboBox() {
        super(() -> Arrays.asList(
            new JavaSourceCodeGenerator(),
            new JsonSourceCodeGenerator(),
            new CurlSourceCodeGenerator()
        ), true);
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof ISourceCodeGenerator) {
            return ((ISourceCodeGenerator) item).getName();
        }
        return super.getItemText(item);
    }
}
