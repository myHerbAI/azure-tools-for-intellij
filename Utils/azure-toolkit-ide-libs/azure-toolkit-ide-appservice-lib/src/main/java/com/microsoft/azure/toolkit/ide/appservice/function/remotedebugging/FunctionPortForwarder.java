/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.remotedebugging;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.azure.toolkit.ide.common.portforwarder.AbstractPortForwarder;
import com.microsoft.azure.toolkit.ide.common.portforwarder.PortForwarderWebSocketListener;
import com.microsoft.azure.toolkit.ide.common.portforwarder.WebSocketBuilder;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.Optional;

public class FunctionPortForwarder extends AbstractPortForwarder {

    private final FunctionAppBase<?, ?, ?> target;

    public FunctionPortForwarder(@Nonnull final FunctionAppBase<?, ?, ?> target) {
        super();
        this.target = target;
    }

    @Override
    public void startForward(int localPort) {
        target.ping();
        super.startForward(localPort);
        // ping function repeatedly otherwise the connection will lose.
        Mono.fromCallable(server::isOpen)
                .delaySubscription(Duration.ofSeconds(30))
                .repeat(server::isOpen)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(ignore -> target.ping(), throwable -> {});
    }

    @Override
    protected WebSocketBuilder createSocketBuilder(OkHttpClient httpClient) {
        return new WebSocketBuilder(httpClient)
            .uri(String.format("wss://%s/DebugSiteExtension/JavaDebugSiteExtension.ashx", target.getKuduHostName()))
            .header("Authorization", getCredential(target))
            .header("Cache-Control", "no-cache");
    }

    @Override
    protected PortForwarderWebSocketListener createWebSocketListener(SocketChannel socketChannel) {
        return new PortForwarderWebSocketListener(socketChannel, socketChannel, this);
    }

    private static String getCredential(final FunctionAppBase<?, ?, ?> app) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = (new TokenRequestContext()).addScopes(scopes);
        final AccessToken token = account.getTokenCredential(app.getSubscriptionId()).getToken(request).block();
        return Optional.ofNullable(token).map(AccessToken::getToken).map(t -> "Bearer " + t).orElse(null);
    }
}
