/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.artifact;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.core.actions.MavenExecuteAction;
import com.microsoft.azuretools.core.utils.MavenUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class AzureArtifactManager {
    private static final String ARTIFACT_NOT_SUPPORTED = "Target file does not exist or is not executable, please " +
        "check the files under folder `%s`.";
    private static final String MULTI_ARTIFACT = "Multiple deployable artifacts(%s) are found at folder `%s`, deploy terminates.";
    private static final String MAVEN_GOALS = "package";

    private static class AzureArtifactManagerHolder {
        private static final AzureArtifactManager instance = new AzureArtifactManager();
    }

    public static AzureArtifactManager getInstance() {
        return AzureArtifactManagerHolder.instance;
    }

    private AzureArtifactManager() {
    }

    public static Mono<IStatus> buildArtifact(AzureArtifact artifact) {
        return Mono.create((sink) -> {
            final Object ref = artifact.getReferencedObject();
            if (ref instanceof MavenProject) {
                Path path = new Path(((MavenProject) ref).getBasedir().getAbsolutePath());
                MavenExecuteAction action = new MavenExecuteAction(MAVEN_GOALS);
                IContainer container = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(path);
                try {
                    action.launch(container, () -> {
                        sink.success(Status.OK_STATUS);
                    }, () -> {
                        sink.error(new AzureToolkitRuntimeException("Fail to build the maven project: " + ((MavenProject) ref).getName()));
                    });
                } catch (CoreException e) {
                    sink.error(e);
                }
            }
        });
    }

    public AzureArtifact getAzureArtifactById(AzureArtifactType type, String artifactId) {
        return type == AzureArtifactType.File ? AzureArtifact.createFromFile(artifactId) :
            getAllSupportedAzureArtifacts().stream().filter(artifact -> StringUtils.equals(artifact.getArtifactIdentifier()
                , artifactId)).findFirst().orElse(null);
    }

    public List<AzureArtifact> getAllSupportedAzureArtifacts() {
        return prepareAzureArtifacts(null);
    }

    private List<AzureArtifact> prepareAzureArtifacts(Predicate<String> packagingFilter) {
        final List<AzureArtifact> azureArtifacts = new ArrayList<>();

        final List<MavenProject> mavenProjects = listMavenProjects();
        azureArtifacts.addAll(mavenProjects.stream().map(AzureArtifact::createFromMavenProject).collect(Collectors.toList()));

        if (packagingFilter == null) {
            return azureArtifacts;
        }
        return azureArtifacts.stream().filter(artifact -> packagingFilter.test(getPackaging(artifact))).collect(Collectors.toList());

    }

    public static List<MavenProject> listMavenProjects() {
        return listJavaProjects().stream().filter(MavenUtils::isMavenProject)
            .map(AzureArtifactManager::toMavenProject).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static MavenProject toMavenProject(IProject project) {
        final IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();
        final IMavenProjectFacade projectFacade = projectRegistry.create(project, new NullProgressMonitor());

        if (projectFacade == null) {
            return null;
        }

        try {
            return projectFacade.getMavenProject(new NullProgressMonitor());
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<IProject> listJavaProjects() {
        List<IProject> projectList = new ArrayList<>();
        try {
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = workspaceRoot.getProjects();
            for (int i = 0; i < projects.length; i++) {
                IProject project = projects[i];
                if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                    projectList.add(project);
                }
            }
        } catch (CoreException e) {
            throw new AzureToolkitRuntimeException("Cannot list java projects.", e);
        }
        return projectList;
    }

    public String getPackaging(AzureArtifact artifact) {
        switch (artifact.getType()) {
            case Maven:
                return ((MavenProject) artifact.getReferencedObject()).getPackaging();
            case File:
                return getExtension(((File) artifact.getReferencedObject()).getName());
            default:
                return null;
        }
    }

    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        } else {
            String name = (new File(filename)).getName();
            int extensionPosition = name.lastIndexOf(46);
            return extensionPosition < 0 ? "" : name.substring(extensionPosition + 1);
        }
    }

    @AzureOperation(
        name = "internal/common.get_artifact_file.artifact",
        params = {"artifact.getName()"}
    )
    public File getFileForDeployment(AzureArtifact artifact) {
        switch (artifact.getType()) {
            case Maven:
                return getArtifactFromMavenProject((MavenProject) artifact.getReferencedObject());
            case File:
                return (File) artifact.getReferencedObject();
            default:
                return null;
        }
    }

    protected static File getArtifactFromMavenProject(MavenProject project) {
        File dir = new File(project.getBuild().getDirectory());
        Collection<File> files = FileUtils.listFiles(dir, new String[]{project.getPackaging()}, false);
        if (files.isEmpty()) {
            throw new AzureToolkitRuntimeException("Cannot find deployable files in target path:" + dir.getAbsolutePath());
        }
        if (StringUtils.equalsIgnoreCase(project.getPackaging(), "jar")) {
            return getExecutableJarFiles(dir, files);
        }
        if (files.size() > 1) {
            final String artifactNameLists = files.stream()
                .map(File::getName).collect(Collectors.joining(","));
            throw new AzureToolkitRuntimeException(String.format(MULTI_ARTIFACT, artifactNameLists, dir.getAbsolutePath()));
        }
        return files.iterator().next();
    }

    protected static File getExecutableJarFiles(File dir, Collection<File> files) throws AzureToolkitRuntimeException {
        if (files.isEmpty()) {
            return null;
        }
        final List<File> executableJars = files.stream().filter(AzureArtifactManager::isExecutableJar).collect(Collectors.toList());
        if (executableJars.isEmpty()) {
            throw new AzureToolkitRuntimeException(String.format(ARTIFACT_NOT_SUPPORTED, dir.getAbsolutePath()));
        }
        // Throw exception when there are multi runnable artifacts
        if (executableJars.size() > 1) {
            final String artifactNameLists = executableJars.stream()
                .map(File::getName).collect(Collectors.joining(","));
            throw new AzureToolkitRuntimeException(String.format(MULTI_ARTIFACT, artifactNameLists, dir.getAbsolutePath()));
        }
        return executableJars.get(0);
    }

    public static boolean isExecutableJar(File file) {
        try (final FileInputStream fileInputStream = new FileInputStream(file);
             final JarInputStream jarInputStream = new JarInputStream(fileInputStream)) {
            final Manifest manifest = jarInputStream.getManifest();
            return manifest.getMainAttributes().getValue("Main-Class") != null;
        } catch (IOException e) {
            return false;
        }
    }
}

