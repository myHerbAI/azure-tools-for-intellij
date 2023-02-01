/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import org.apache.http.impl.client.HttpClientBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ProxyUtils {
    public static void initProxy() {
        final HttpConfigurable instance = HttpConfigurable.getInstance();
        if (instance != null && instance.USE_HTTP_PROXY) {
            final ProxyInfo proxy = ProxyInfo.builder()
                .source("intellij")
                .host(instance.PROXY_HOST)
                .port(instance.PROXY_PORT)
                .username(instance.getProxyLogin())
                .password(instance.getPlainProxyPassword())
                .build();
            Azure.az().config().setProxyInfo(proxy);
            ProxyManager.getInstance().applyProxy();
        }
        setSslContext();
        setTrustStoreProperties();
    }

    private static void setSslContext() {
        final CertificateManager certificateManager = CertificateManager.getInstance();
        Azure.az().config().setSslContext(certificateManager.getSslContext());
    }

    private static void setTrustStoreProperties() {
        final CertificateManager certificateManager = CertificateManager.getInstance();
        final String javaHome = System.getenv("JAVA_HOME");
        Optional.ofNullable(javaHome).ifPresent(it -> {
            final Path trustStoreFilePath = Paths.get(it,  "lib", "security", "cacerts");
            if (Files.exists(trustStoreFilePath)) {
                System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath.toString());
                System.setProperty("javax.net.ssl.trustStorePassword", certificateManager.getPassword());
            }
        });
    }
}
