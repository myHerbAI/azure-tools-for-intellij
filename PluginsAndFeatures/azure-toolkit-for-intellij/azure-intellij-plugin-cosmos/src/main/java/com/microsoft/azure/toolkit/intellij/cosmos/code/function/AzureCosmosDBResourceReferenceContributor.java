package com.microsoft.azure.toolkit.intellij.cosmos.code.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionStringLiteralResourceReference;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainer;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBContainerNameCompletionProvider.COSMOS_CONTAINER_NAME_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBDatabaseNameCompletionProvider.COSMOS_DATABASE_NAME_PAIR_PATTERN;

public class AzureCosmosDBResourceReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(COSMOS_DATABASE_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class));
                final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
                final SqlDatabase database = CosmosDBDatabaseNameCompletionProvider.getConnectedDatabase(annotation);
                if (Objects.nonNull(database) && StringUtils.equalsIgnoreCase(value, database.getName())) {
                    final TextRange range = new TextRange(value.indexOf(database.getName()) + 1, value.indexOf(database.getName()) + 1 + database.getName().length());
                    return new PsiReference[]{new FunctionStringLiteralResourceReference(element, range, database, connection, true)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(COSMOS_CONTAINER_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class));
                final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
                final SqlDatabase database = CosmosDBDatabaseNameCompletionProvider.getConnectedDatabase(annotation);
                final SqlContainer container = database.containers().get(value, database.getResourceGroupName());
                if (Objects.nonNull(container)) {
                    final TextRange range = new TextRange(value.indexOf(container.getName()) + 1, value.indexOf(container.getName()) + 1 + database.getName().length());
                    return new PsiReference[]{new FunctionStringLiteralResourceReference(element, range, container, connection, true)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}
