/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collection;

import static com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule.DOT_AZURE;
import static com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule.PROFILES_XML;

public class AzureFacetType extends FacetType<AzureFacet, AzureFacetConfiguration> {
    private static final String STRING_ID = "azure";
    private static final String PRESENTABLE_NAME = "Azure";
    public static final FacetTypeId<AzureFacet> ID = new FacetTypeId<>(STRING_ID);
    public static final AzureFacetType INSTANCE = new AzureFacetType();

    public AzureFacetType() {
        super(ID, STRING_ID, PRESENTABLE_NAME);
    }

    @Override
    public AzureFacetConfiguration createDefaultConfiguration() {
        return new AzureFacetConfiguration();
    }

    @Override
    public AzureFacet createFacet(@Nonnull Module module, String name, @Nonnull AzureFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new AzureFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return true;
    }

    @Override
    public boolean isOnlyOneFacetAllowed() {
        return true;
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE);
    }

    @Getter
    public static class AzureFacetDetector extends FacetBasedFrameworkDetector<AzureFacet, AzureFacetConfiguration> {
        private final FacetType<AzureFacet, AzureFacetConfiguration> facetType = AzureFacetType.INSTANCE;
        private final FileType fileType = XmlFileType.INSTANCE;

        public AzureFacetDetector() {
            super(STRING_ID);
        }

        @Override
        @Nullable
        protected AzureFacetConfiguration createConfiguration(Collection<? extends VirtualFile> files) {
            return files.stream().findAny().map(VirtualFile::getParent).map(AzureFacetConfiguration::new).orElse(null);
        }

        @Nonnull
        @Override
        public ElementPattern<FileContent> createSuitableFilePattern() {
            return FileContentPattern.fileContent().inDirectory(DOT_AZURE).withName(PROFILES_XML);
        }
    }
}