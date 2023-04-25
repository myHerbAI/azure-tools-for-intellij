/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.container;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.container.model.DockerHost.DEFAULT_WINDOWS_HOST;

public class AzureDockerClient {
    public static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private final DefaultDockerClientConfig config;
    private final DockerClient client;

    private static AzureDockerClient DEFAULT = null;

    private AzureDockerClient() {
        this.config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        final DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .connectionTimeout(CONNECTION_TIMEOUT)
            .build();
        this.client = DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
    }

    private AzureDockerClient(String dockerHost, boolean tlsEnabled, String certPath) {
        final DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(dockerHost);
        if (tlsEnabled) {
            builder.withDockerCertPath(certPath).build();
        }
        this.config = builder.build();
        final ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .connectionTimeout(CONNECTION_TIMEOUT)
            .build();
        this.client = DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
    }

    public static AzureDockerClient from(@Nonnull final DockerHost dockerHost) {
        return from(dockerHost.getDockerHost(), dockerHost.isTlsEnabled(), dockerHost.getDockerCertPath());
    }

    public static AzureDockerClient from(String dockerHost, boolean tlsEnabled, String certPath) {
        return new AzureDockerClient(dockerHost, tlsEnabled, certPath);
    }

    public static synchronized AzureDockerClient getDefault() {
        if (DEFAULT == null) {
            DEFAULT = new AzureDockerClient();
        }
        return DEFAULT;
    }

    @AzureOperation(name = "boundary/docker.create_container.image", params = {"imageNameWithTag"})
    public String createContainer(@Nonnull String imageNameWithTag, String port) {
        this.ping();
        final InspectImageResponse image = this.client.inspectImageCmd(imageNameWithTag).exec();
        final ContainerConfig config = Objects.requireNonNull(image.getConfig());
        final List<PortBinding> portBindings = Arrays.stream(ObjectUtils.firstNonNull(config.getExposedPorts(), new ExposedPort[0]))
            .map(p -> new PortBinding(Ports.Binding.bindPort(findFreePort()), p)).collect(Collectors.toList());
        if (StringUtils.isNotEmpty(port)) {
            portBindings.add(new PortBinding(Ports.Binding.bindPort(findFreePort()), ExposedPort.parse(port)));
        }
        //noinspection deprecation
        final CreateContainerResponse container = this.client.createContainerCmd(imageNameWithTag).withPortBindings(portBindings).exec();
        return container.getId();
    }

    @AzureOperation(name = "boundary/docker.run_container.container", params = {"containerId"})
    public Container runContainer(@Nonnull String containerId) {
        this.ping();
        this.client.startContainerCmd(containerId).exec();
        final List<Container> containers = this.client.listContainersCmd().exec();
        return containers.stream().filter(item -> item.getId().equals(containerId)).findFirst()
            .orElseThrow(() -> new DockerException("Error in starting container.", 404));
    }

    @AzureOperation(name = "boundary/docker.stop_container.container", params = {"containerId"})
    public void stopContainer(@Nonnull String containerId) {
        this.ping();
        this.client.stopContainerCmd(containerId).exec();
        this.client.removeContainerCmd(containerId).exec();
    }

    @AzureOperation(name = "boundary/docker.build_image.image|dir", params = {"imageNameWithTag", "baseDir"})
    public void buildImage(String imageNameWithTag, @Nonnull File dockerFile, File baseDir, @Nullable BuildImageResultCallback callback) {
        this.ping();
        baseDir = Optional.ofNullable(baseDir).orElseGet(dockerFile::getParentFile);
        final String imageId = this.client.buildImageCmd()
            .withDockerfile(dockerFile)
            .withBaseDirectory(baseDir)
            .withTags(Set.of(imageNameWithTag))
            .exec(Optional.ofNullable(callback).orElseGet(BuildImageResultCallback::new)).awaitImageId();
    }

    @AzureOperation(name = "boundary/docker.push_image.image|registry", params = {"targetImageName", "registryUrl"})
    public void pushImage(@Nonnull String registryUrl, String username, String password, @Nonnull String targetImageName, @Nullable ResultCallback.Adapter<PushResponseItem> callback)
        throws InterruptedException {
        this.ping();
        final AuthConfig authConfig = new AuthConfig().withUsername(username).withPassword(password).withRegistryAddress(registryUrl);
        final PushImageCmd cmd = this.client.pushImageCmd(targetImageName).withAuthConfig(authConfig);
        cmd.exec(Optional.ofNullable(callback).orElseGet(ResultCallback.Adapter::new)).awaitCompletion();
    }

    @AzureOperation(name = "boundary/docker.pull_image.image|registry", params = {"repository", "registryUrl"})
    public void pullImage(@Nonnull String registryUrl, String username, String password, @Nonnull String repository, @Nonnull String tag)
        throws InterruptedException {
        this.ping();
        final AuthConfig authConfig = new AuthConfig().withUsername(username).withPassword(password).withRegistryAddress(registryUrl);
        final String fullRepositoryName = String.format("%s/%s", registryUrl, repository);
        final PullImageCmd cmd = this.client.pullImageCmd(fullRepositoryName).withRegistry(registryUrl).withTag(tag).withAuthConfig(authConfig);
        final ResultCallback.Adapter<PullResponseItem> adapter = cmd.exec(new ResultCallback.Adapter<>()).awaitCompletion();
    }

    @AzureOperation(name = "boundary/docker.inspect_image.image", params = {"imageId"})
    public InspectImageResponse inspectImage(String imageId) {
        this.ping();
        return this.client.inspectImageCmd(imageId).exec();
    }

    public void tagImage(String imageName, String fullRepositoryName, String tagName) {
        this.ping();
        this.client.tagImageCmd(imageName, fullRepositoryName, tagName).exec();
    }

    public List<Image> listLocalImages() {
        this.ping();
        return this.client.listImagesCmd().withDanglingFilter(false).exec();
    }

    @AzureOperation(name = "boundary/docker.find_image.image|host", params = {"tag.getImageName()", "this.config.getDockerHost().toString()"})
    public Image getImage(Tag tag) {
        this.ping();
        final List<Image> images = this.client.listImagesCmd().exec();
        return images.stream()
            .filter(i -> ArrayUtils.isNotEmpty(i.getRepoTags()))
            .filter(i -> Arrays.stream(i.getRepoTags()).anyMatch(t -> t.equalsIgnoreCase(tag.getFullName())))
            .findAny().orElse(null);
    }

    @AzureOperation(name = "boundary/docker.ping_host.host", params = {"this.config.getDockerHost().toString()"})
    public void ping() {
        try {
            this.client.pingCmd().exec();
        } catch (final Throwable t) {
            final String message = String.format("failed to ping docker host at \"%s\"", config.getDockerHost());
            throw new AzureToolkitRuntimeException(message, t, "Docker host is not running or Docker is not installed.");
        }
    }

    public static ObjectMapper getDefaultObjectMapper() {
        return DockerClientConfig.getDefaultObjectMapper();
    }

    public static void createDockerFile(String baseDir, String filename, String content) throws IOException {
        final File dockerFile = Paths.get(baseDir, filename).toFile();
        if (!dockerFile.exists()) {
            FileUtil.writeToFile(dockerFile, content);
        }
    }

    /**
     * check if the default docker file exists.
     * If yes, return the path as a String.
     * Else return an empty String.
     */
    public static String getDefaultDockerFilePathIfExist(String basePath) {
        try {
            if (!StringUtils.isEmpty(basePath)) {
                final Path targetDockerfile = Paths.get(basePath, Constant.DOCKERFILE_NAME);
                if (targetDockerfile.toFile().exists()) {
                    return targetDockerfile.toString();
                }
            }
        } catch (final RuntimeException ignored) {
        }
        return "";
    }

    // todo: add host configurations from docker extension
    public static List<DockerHost> getDockerHosts() {
        final List<DockerHost> result = new ArrayList<>();
        final DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        final String certPath = config.getSSLConfig() instanceof LocalDirectorySSLConfig ? ((LocalDirectorySSLConfig) config.getSSLConfig()).getDockerCertPath() : null;
        result.add(new DockerHost(config.getDockerHost().toString(), certPath));
        if (SystemInfo.isWindows && !result.contains(DEFAULT_WINDOWS_HOST)) {
            result.add(0, DEFAULT_WINDOWS_HOST);
        }
        return result;
    }

    // todo: move to socket utils
    private static int findFreePort() {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (final IOException e) {
            return -1;
        }
    }
}
