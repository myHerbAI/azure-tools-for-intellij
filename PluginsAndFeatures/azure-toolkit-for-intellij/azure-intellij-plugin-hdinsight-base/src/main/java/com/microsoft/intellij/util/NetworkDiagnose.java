/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.intellij.util;

import com.azure.core.management.AzureEnvironment;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class NetworkDiagnose {

    public static Mono<String> checkAzure(AzureEnvironment env) {
        List<String> urls = new ArrayList<>();
        urls.add("https://www.microsoft.com");
        urls.add(env.getActiveDirectoryEndpoint());
        urls.add(env.getManagementEndpoint());

        return Flux.fromIterable(urls).map(NetworkDiagnose::getDomainName)
            .map(host -> isHostAvailable(host, 443)).filter(StringUtils::isNotBlank)
            .onErrorContinue((throwable, o) -> {
                System.out.println("Cannot check host for:" + o);
            })
            .collectList().map(res -> StringUtils.join(res, ";"));
    }

    @SneakyThrows
    private static String getDomainName(String url) {
        URI uri = new URI(url);
        return uri.getHost();
    }

    private static String isHostAvailable(String hostName, int port) {
        try (Socket socket = new Socket()) {
            InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
            socket.connect(socketAddress, 2000);
            return hostName;
        } catch (IOException e) {
            return StringUtils.EMPTY;
        }
    }
}
