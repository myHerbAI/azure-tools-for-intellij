/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package org.wso2.lsp4intellij.client.languageserver.requestmanager;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.CallHierarchyRegistrationOptions;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.ColorProviderOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DeclarationRegistrationOptions;
import org.eclipse.lsp4j.DefinitionOptions;
import org.eclipse.lsp4j.DiagnosticRegistrationOptions;
import org.eclipse.lsp4j.DocumentFormattingOptions;
import org.eclipse.lsp4j.DocumentHighlightOptions;
import org.eclipse.lsp4j.DocumentLinkOptions;
import org.eclipse.lsp4j.DocumentOnTypeFormattingOptions;
import org.eclipse.lsp4j.DocumentRangeFormattingOptions;
import org.eclipse.lsp4j.DocumentSymbolOptions;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.FoldingRangeProviderOptions;
import org.eclipse.lsp4j.HoverOptions;
import org.eclipse.lsp4j.ImplementationRegistrationOptions;
import org.eclipse.lsp4j.InlayHintRegistrationOptions;
import org.eclipse.lsp4j.InlineValueRegistrationOptions;
import org.eclipse.lsp4j.LinkedEditingRangeRegistrationOptions;
import org.eclipse.lsp4j.MonikerRegistrationOptions;
import org.eclipse.lsp4j.NotebookDocumentSyncRegistrationOptions;
import org.eclipse.lsp4j.ReferenceOptions;
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.SelectionRangeRegistrationOptions;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.TypeDefinitionRegistrationOptions;
import org.eclipse.lsp4j.TypeHierarchyRegistrationOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.WorkspaceSymbolOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.wso2.lsp4intellij.client.DefaultLanguageClient;
import org.wso2.lsp4intellij.client.DynamicRegistrationMethods;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

import static org.wso2.lsp4intellij.client.DynamicRegistrationMethods.COMPLETION;
import static org.wso2.lsp4intellij.client.DynamicRegistrationMethods.DEFINITION;
import static org.wso2.lsp4intellij.client.DynamicRegistrationMethods.HOVER;
import static org.wso2.lsp4intellij.client.DynamicRegistrationMethods.SIGNATURE_HELP;

public class DynamicServerCapability extends ServerCapabilities {
    private static final Gson GSON = new Gson();
    private final @Nonnull ServerCapabilities serverCapabilities;
    private final @Nonnull Map<String, Pair<DynamicRegistrationMethods, Object>> registrations;

    public DynamicServerCapability(@Nonnull final ServerCapabilities serverCapabilities, @Nonnull final DefaultLanguageClient languageClient) {
        this.serverCapabilities = serverCapabilities;
        this.registrations = languageClient.getRegistrations();
    }

    @Override
    public String getPositionEncoding() {
        return serverCapabilities.getPositionEncoding();
    }

    @Override
    public Either<TextDocumentSyncKind, TextDocumentSyncOptions> getTextDocumentSync() {
        return serverCapabilities.getTextDocumentSync();
    }

    @Override
    public NotebookDocumentSyncRegistrationOptions getNotebookDocumentSync() {
        return serverCapabilities.getNotebookDocumentSync();
    }

    @Override
    public WorkspaceServerCapabilities getWorkspace() {
        return serverCapabilities.getWorkspace();
    }

    @Override
    public Object getExperimental() {
        return serverCapabilities.getExperimental();
    }

    @Override
    public Either<Boolean, HoverOptions> getHoverProvider() {
        return Optional.ofNullable(getCapabilityPair(HOVER))
                .map(pair -> convertToEitherOptions(pair, HoverOptions.class))
                .orElse(serverCapabilities.getHoverProvider());
    }

    @Override
    public CompletionOptions getCompletionProvider() {
        return Optional.ofNullable(getCapabilityPair(COMPLETION))
                .map(pair -> convertToOptions(pair, CompletionOptions.class))
                .orElse(serverCapabilities.getCompletionProvider());
    }

    @Override
    public SignatureHelpOptions getSignatureHelpProvider() {
        return Optional.ofNullable(getCapabilityPair(SIGNATURE_HELP))
                .map(pair -> convertToOptions(pair, SignatureHelpOptions.class))
                .orElse(serverCapabilities.getSignatureHelpProvider());
    }

    @Override
    public Either<Boolean, DefinitionOptions> getDefinitionProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, DefinitionOptions.class))
                .orElse(serverCapabilities.getDefinitionProvider());
    }

    @Override
    public Either<Boolean, TypeDefinitionRegistrationOptions> getTypeDefinitionProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, TypeDefinitionRegistrationOptions.class))
                .orElse(serverCapabilities.getTypeDefinitionProvider());
    }

    @Override
    public Either<Boolean, ImplementationRegistrationOptions> getImplementationProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, ImplementationRegistrationOptions.class))
                .orElse(serverCapabilities.getImplementationProvider());
    }

    @Override
    public Either<Boolean, ReferenceOptions> getReferencesProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, ReferenceOptions.class))
                .orElse(serverCapabilities.getReferencesProvider());
    }

    @Override
    public Either<Boolean, DocumentHighlightOptions> getDocumentHighlightProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, DocumentHighlightOptions.class))
                .orElse(serverCapabilities.getDocumentHighlightProvider());
    }

    @Override
    public Either<Boolean, DocumentSymbolOptions> getDocumentSymbolProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, DocumentSymbolOptions.class))
                .orElse(serverCapabilities.getDocumentSymbolProvider());
    }

    @Override
    public Either<Boolean, WorkspaceSymbolOptions> getWorkspaceSymbolProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, WorkspaceSymbolOptions.class))
                .orElse(serverCapabilities.getWorkspaceSymbolProvider());
    }

    @Override
    public Either<Boolean, CodeActionOptions> getCodeActionProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, CodeActionOptions.class))
                .orElse(serverCapabilities.getCodeActionProvider());
    }

    @Override
    public CodeLensOptions getCodeLensProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToOptions(pair, CodeLensOptions.class))
                .orElse(serverCapabilities.getCodeLensProvider());
    }

    @Override
    public Either<Boolean, DocumentFormattingOptions> getDocumentFormattingProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, DocumentFormattingOptions.class))
                .orElse(serverCapabilities.getDocumentFormattingProvider());
    }

    @Override
    public Either<Boolean, DocumentRangeFormattingOptions> getDocumentRangeFormattingProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, DocumentRangeFormattingOptions.class))
                .orElse(serverCapabilities.getDocumentRangeFormattingProvider());
    }

    @Override
    public DocumentOnTypeFormattingOptions getDocumentOnTypeFormattingProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToOptions(pair, DocumentOnTypeFormattingOptions.class))
                .orElse(serverCapabilities.getDocumentOnTypeFormattingProvider());
    }

    @Override
    public Either<Boolean, RenameOptions> getRenameProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, RenameOptions.class))
                .orElse(serverCapabilities.getRenameProvider());
    }

    @Override
    public DocumentLinkOptions getDocumentLinkProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToOptions(pair, DocumentLinkOptions.class))
                .orElse(serverCapabilities.getDocumentLinkProvider());
    }

    @Override
    public Either<Boolean, ColorProviderOptions> getColorProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, ColorProviderOptions.class))
                .orElse(serverCapabilities.getColorProvider());
    }

    @Override
    public Either<Boolean, FoldingRangeProviderOptions> getFoldingRangeProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, FoldingRangeProviderOptions.class))
                .orElse(serverCapabilities.getFoldingRangeProvider());
    }

    @Override
    public Either<Boolean, DeclarationRegistrationOptions> getDeclarationProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, DeclarationRegistrationOptions.class))
                .orElse(serverCapabilities.getDeclarationProvider());
    }

    @Override
    public ExecuteCommandOptions getExecuteCommandProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToOptions(pair, ExecuteCommandOptions.class))
                .orElse(serverCapabilities.getExecuteCommandProvider());
    }

    @Override
    public Either<Boolean, TypeHierarchyRegistrationOptions> getTypeHierarchyProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, TypeHierarchyRegistrationOptions.class))
                .orElse(serverCapabilities.getTypeHierarchyProvider());
    }

    @Override
    public Either<Boolean, CallHierarchyRegistrationOptions> getCallHierarchyProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, CallHierarchyRegistrationOptions.class))
                .orElse(serverCapabilities.getCallHierarchyProvider());
    }

    @Override
    public Either<Boolean, SelectionRangeRegistrationOptions> getSelectionRangeProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, SelectionRangeRegistrationOptions.class))
                .orElse(serverCapabilities.getSelectionRangeProvider());
    }

    @Override
    public Either<Boolean, LinkedEditingRangeRegistrationOptions> getLinkedEditingRangeProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, LinkedEditingRangeRegistrationOptions.class))
                .orElse(serverCapabilities.getLinkedEditingRangeProvider());
    }

    @Override
    public SemanticTokensWithRegistrationOptions getSemanticTokensProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToOptions(pair, SemanticTokensWithRegistrationOptions.class))
                .orElse(serverCapabilities.getSemanticTokensProvider());
    }

    @Override
    public Either<Boolean, MonikerRegistrationOptions> getMonikerProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, MonikerRegistrationOptions.class))
                .orElse(serverCapabilities.getMonikerProvider());
    }

    @Override
    public Either<Boolean, InlayHintRegistrationOptions> getInlayHintProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, InlayHintRegistrationOptions.class))
                .orElse(serverCapabilities.getInlayHintProvider());
    }

    @Override
    public Either<Boolean, InlineValueRegistrationOptions> getInlineValueProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToEitherOptions(pair, InlineValueRegistrationOptions.class))
                .orElse(serverCapabilities.getInlineValueProvider());
    }

    @Override
    public DiagnosticRegistrationOptions getDiagnosticProvider() {
        return Optional.ofNullable(getCapabilityPair(DEFINITION))
                .map(pair -> convertToOptions(pair, DiagnosticRegistrationOptions.class))
                .orElse(serverCapabilities.getDiagnosticProvider());
    }


    @Nullable
    private static <T> Either<Boolean, T> convertToEitherOptions(@Nonnull final Pair<DynamicRegistrationMethods, Object> pair, Class<T> tClass) {
        return Optional.ofNullable(convertToOptions(pair.getRight(), tClass))
                .map(Either::<Boolean, T>forRight)
                .orElse(null);
    }

    @Nullable
    private static <T> T convertToOptions(@Nonnull final Object object, Class<T> tClass) {
        try {
            return GSON.fromJson(GSON.toJson(object), tClass);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private Pair<DynamicRegistrationMethods, Object> getCapabilityPair(@Nonnull final DynamicRegistrationMethods method) {
        return registrations.values().stream()
                .filter(pair -> pair.getKey() == method)
                .findFirst().orElse(null);
    }
}
