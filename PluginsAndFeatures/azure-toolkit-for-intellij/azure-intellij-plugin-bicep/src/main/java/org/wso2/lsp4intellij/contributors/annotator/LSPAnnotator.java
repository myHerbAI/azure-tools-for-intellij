/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * Modifications copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.lsp4intellij.contributors.annotator;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.contributors.fixes.LSPCodeActionFix;
import org.wso2.lsp4intellij.contributors.fixes.LSPCommandFix;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.CODEACTION;

public class LSPAnnotator extends ExternalAnnotator<Object, Object> {

    private static final Logger LOG = Logger.getInstance(LSPAnnotator.class);
    private static final Object RESULT = new Object();
    private static final HashMap<DiagnosticSeverity, HighlightSeverity> lspToIntellijAnnotationsMap = new HashMap<>();

    static {
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Error, HighlightSeverity.ERROR);
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Warning, HighlightSeverity.WARNING);

        // seem flipped, but just different semantics lsp<->intellij. Hint is rendered without any squiggle
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Information, HighlightSeverity.WEAK_WARNING);
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Hint, HighlightSeverity.INFORMATION);
    }

    @Nullable
    @Override
    public Object collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {

        try {
            VirtualFile virtualFile = file.getVirtualFile();

            // If the file is not supported, we skips the annotation by returning null.
            if (!FileUtils.isFileSupported(virtualFile) || !IntellijLanguageClient.isExtensionSupported(virtualFile)) {
                return null;
            }
            EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);

            if (eventManager == null) {
                return null;
            }

            return RESULT;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    @Override
    public Object doAnnotate(Object collectedInfo) {
        return RESULT;
    }

    @Override
    public void apply(@NotNull PsiFile file, Object annotationResult, @NotNull AnnotationHolder holder) {
        final LanguageServerWrapper wrapper = LanguageServerWrapper.forVirtualFile(file.getVirtualFile(), file.getProject());
        if (Objects.nonNull(wrapper) && wrapper.getStatus() != ServerStatus.INITIALIZED) {
            return;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        if (FileUtils.isFileSupported(virtualFile) && IntellijLanguageClient.isExtensionSupported(virtualFile)) {
            String uri = FileUtils.VFSToURI(virtualFile);
            // TODO annotations are applied to a file / document not to an editor. so store them by file and not by editor..
            EditorEventManager eventManager = EditorEventManagerBase.forUri(uri);

            try {
                createAnnotations(holder, eventManager);
            } catch (ConcurrentModificationException e) {
                // Todo - Add proper fix to handle concurrent modifications gracefully.
                LOG.warn("Error occurred when updating LSP code actions due to concurrent modifications.", e);
            } catch (Throwable t) {
                LOG.warn("Error occurred when updating LSP code actions.", t);
            }
        }
    }

    private void createAnnotations(AnnotationHolder holder, EditorEventManager eventManager) {
        final List<Diagnostic> diagnostics = eventManager.getDiagnostics();
        final Editor editor = eventManager.editor;
        diagnostics.forEach(diagnostic -> {
            final int start = DocumentUtils.LSPPosToOffset(editor, diagnostic.getRange().getStart());
            final int end = DocumentUtils.LSPPosToOffset(editor, diagnostic.getRange().getEnd());
            if (start >= end) {
                return;
            }
            final TextRange range = new TextRange(start, end);
            final AnnotationBuilder annotationBuilder = holder.newAnnotation(lspToIntellijAnnotationsMap.get(diagnostic.getSeverity()), diagnostic.getMessage())
                    .range(range);
            if (diagnostic.getTags() != null && diagnostic.getTags().contains(DiagnosticTag.Deprecated)) {
                annotationBuilder.highlightType(ProblemHighlightType.LIKE_DEPRECATED);
            }
            annotationBuilder.create();
        });
        requestAndShowCodeActions(holder, eventManager);
    }

    private void requestAndShowCodeActions(final AnnotationHolder holder, final EditorEventManager eventManager) {
        final Editor editor = eventManager.editor;
        final List<Annotation> diagnostics = (holder instanceof SmartList) ? (List<Annotation>) holder : Collections.emptyList();
        final int caretPos = editor.getCaretModel().getCurrentCaret().getOffset();
        final List<Either<Command, CodeAction>> codeActionResp = codeAction(eventManager, caretPos);
        if (codeActionResp == null || codeActionResp.isEmpty()) {
            return;
        }
        final AtomicBoolean codeActionSyncRequired = new AtomicBoolean(false);
        codeActionResp.stream().filter(Objects::nonNull).forEach(element -> {
            if (element.isLeft()) {
                Command command = element.getLeft();
                diagnostics.forEach(annotation -> {
                    int start = annotation.getStartOffset();
                    int end = annotation.getEndOffset();
                    if (start <= caretPos && end >= caretPos) {
                        annotation.registerFix(new LSPCommandFix(FileUtils.editorToURIString(editor), command), new TextRange(start, end));
                    }
                    codeActionSyncRequired.set(true);
                });
            } else if (element.isRight()) {
                CodeAction codeAction = element.getRight();
                List<Diagnostic> diagnosticContext = codeAction.getDiagnostics();
                diagnostics.forEach(annotation -> {
                    int start = annotation.getStartOffset();
                    int end = annotation.getEndOffset();
                    if (start <= caretPos && end >= caretPos) {
                        annotation.registerFix(new LSPCodeActionFix(FileUtils.editorToURIString(editor), codeAction), new TextRange(start, end));
                    }
                    codeActionSyncRequired.set(true);
                });

                // If the code actions does not have a diagnostics context, creates an intention action for
                // the current line.
                if (CollectionUtils.isEmpty(diagnosticContext) && holder != null) {
                    // Calculates text range of the current line.
                    int line = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line;
                    int startOffset = editor.getDocument().getLineStartOffset(line);
                    int endOffset = editor.getDocument().getLineEndOffset(line);
                    TextRange range = new TextRange(startOffset, endOffset);
                    holder.newAnnotation(HighlightSeverity.INFORMATION, codeAction.getTitle())
                            .withFix(new LSPCodeActionFix(FileUtils.editorToURIString(editor), codeAction))
                            .range(range)
                            .create();
                }
            }
        });
    }

    /**
     * Retrieves the commands needed to apply a CodeAction
     *
     * @param offset The cursor position(offset) which should be evaluated for code action request.
     * @return The list of commands, or null if none are given / the request times out
     */
    @SuppressWarnings("WeakerAccess")
    public List<Either<Command, CodeAction>> codeAction(final EditorEventManager eventManager, final int offset) {
        final CodeActionParams params = new CodeActionParams();
        final Editor editor = eventManager.editor;
        params.setTextDocument(eventManager.getIdentifier());
        Range range = new Range(DocumentUtils.offsetToLSPPos(editor, offset),
                DocumentUtils.offsetToLSPPos(editor, offset));
        params.setRange(range);

        // Calculates the diagnostic context.
        List<Diagnostic> diagnosticContext = new ArrayList<>();
        synchronized (eventManager.getDiagnostics()) {
            eventManager.getDiagnostics().forEach(diagnostic -> {
                int startOffset = DocumentUtils.LSPPosToOffset(editor, diagnostic.getRange().getStart());
                int endOffset = DocumentUtils.LSPPosToOffset(editor, diagnostic.getRange().getEnd());
                if (offset >= startOffset && offset <= endOffset) {
                    diagnosticContext.add(diagnostic);
                }
            });
        }

        final CodeActionContext context = new CodeActionContext(diagnosticContext);
        params.setContext(context);
        final LanguageServerWrapper wrapper = eventManager.wrapper;
        final CompletableFuture<List<Either<Command, CodeAction>>> future = eventManager.wrapper.getRequestManager().codeAction(params);
        if (future != null) {
            try {
                List<Either<Command, CodeAction>> res = future.get(getTimeout(CODEACTION), TimeUnit.MILLISECONDS);
                wrapper.notifySuccess(CODEACTION);
                return res;
            } catch (TimeoutException e) {
                LOG.warn(e);
                wrapper.notifyFailure(CODEACTION);
                return null;
            } catch (InterruptedException | JsonRpcException | ExecutionException e) {
                LOG.warn(e);
                wrapper.crashed(e);
                return null;
            }
        }
        return null;
    }
}
