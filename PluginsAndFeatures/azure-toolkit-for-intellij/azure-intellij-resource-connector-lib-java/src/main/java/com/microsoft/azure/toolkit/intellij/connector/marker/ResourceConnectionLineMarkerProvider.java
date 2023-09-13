package com.microsoft.azure.toolkit.intellij.connector.marker;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.*;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class ResourceConnectionLineMarkerProvider implements LineMarkerProvider {

    @Override
    @Nullable
    public LineMarkerInfo<?> getLineMarkerInfo(@Nonnull PsiElement element) {
        if (element instanceof PsiJavaToken && Objects.equals(((PsiJavaToken) element).getTokenType(), JavaTokenType.STRING_LITERAL)) {
            final Connection<AzResource, ?> connection = getBindingConnection((PsiJavaToken) element);
            final AzureServiceResource<?> resource = Optional.ofNullable(connection).filter(c -> c.getResource() instanceof AzureServiceResource)
                    .map(c -> ((AzureServiceResource<?>) c.getResource())).orElse(null);
            if (Objects.nonNull(resource)) {
                return new ResourceConnectionLineMarkerInfo(connection, resource, element);
            }
        }
        return null;
    }

    @Nullable
    public static Connection<AzResource, ?> getBindingConnection(@Nonnull PsiJavaToken element) {
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final String text = StringUtils.strip(element.getText(), "\"");
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile)) {
            return null;
        }
        return (Connection<AzResource, ?>) defaultProfile.getConnections().stream()
                .filter(c -> defaultProfile.getGeneratedEnvironmentVariables(c).stream()
                        .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), text)))
                .findAny().orElse(null);
    }


    private static class ResourceConnectionLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        @Getter
        private Connection<AzResource, ?> connection;

        public ResourceConnectionLineMarkerInfo(@Nonnull Connection<AzResource, ?> connection, final AzureServiceResource<?> resource, @Nonnull PsiElement element) {
            super(element, element.getTextRange(),
                    IntelliJAzureIcons.getIcon(ObjectUtils.firstNonNull(resource.getDefinition().getIcon(), AzureIcons.Connector.CONNECT.getIconPath())),
                    ignore -> String.format("%s (%s)", resource.getName(), resource.getResourceType()),
                    null, null, GutterIconRenderer.Alignment.LEFT, () -> connection.getResource().getName());
            this.connection = connection;
        }

        @Override
        public boolean canMergeWith(@Nonnull MergeableLineMarkerInfo<?> info) {
            return info instanceof ResourceConnectionLineMarkerInfo &&
                    Objects.equals(((ResourceConnectionLineMarkerInfo) info).connection, this.connection);
        }

        @Override
        public Icon getCommonIcon(@Nonnull List<? extends MergeableLineMarkerInfo<?>> infos) {
            return infos.stream().filter(i -> i instanceof ResourceConnectionLineMarkerInfo)
                    .map(i -> (ResourceConnectionLineMarkerInfo) i)
                    .map(ResourceConnectionLineMarkerInfo::getIcon).findFirst().orElse(null);
        }

        @Override
        public GutterIconRenderer createGutterRenderer() {
            return new ResourceConnectionGutterIconRender(this, connection);
        }
    }

    private static class ResourceConnectionGutterIconRender extends LineMarkerGutterIconRenderer<PsiElement> {
        private final Connection<AzResource, ?> connection;
        private final LineMarkerInfo<PsiElement> info;
        @Getter
        private AnAction clickAction;
        private AnAction editConnectionAction;
        private AnAction editEnvAction;
        @Getter
        private ActionGroup popupMenuActions;

        public ResourceConnectionGutterIconRender(@Nonnull ResourceConnectionLineMarkerInfo info, Connection<AzResource, ?> connection) {
            super(info);
            this.info = info;
            this.connection = connection;
            this.initActions();
        }

        private void initActions() {
            this.clickAction = new AnAction("Navigate", "Navigate to resource in explorer", this.getIcon()) {
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    final Predicate<DefaultMutableTreeNode> rootPredict = node -> node.getUserObject() instanceof AzureFacetRootNode;
                    final Predicate<DefaultMutableTreeNode> connectionsPredict = node -> node.getUserObject() instanceof ConnectionsNode;
                    final Predicate<DefaultMutableTreeNode> connectionPredict = node -> node.getUserObject() instanceof ConnectionNode
                            && Objects.equals(((ConnectionNode) node.getUserObject()).getValue(), connection);
                    final Predicate<DefaultMutableTreeNode> resourcePredict = node -> node.getUserObject() instanceof ResourceNode
                            && Objects.equals(((ResourceNode) node.getUserObject()).getValue().getValue(), connection.getResource().getData());
                    AbstractAzureFacetNode.focusNode(info.getElement().getProject(), resourcePredict, connectionPredict, connectionsPredict, rootPredict);
                }
            };
            this.editConnectionAction = new AnAction("Edit Connection", "Edit resource connection for current resource", AllIcons.Actions.Edit){
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    AzureActionManager.getInstance().getAction(ResourceConnectionActionsContributor.EDIT_CONNECTION).handle(connection, e);
                }
            };
            this.editEnvAction = new AnAction("Edit Environment Variables", "Edit environment variables for current resource", AllIcons.Actions.Edit){
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    AzureActionManager.getInstance().getAction(ResourceConnectionActionsContributor.EDIT_ENV_FILE_IN_EDITOR).handle(connection, e);
                }
            };
            this.popupMenuActions = new ActionGroup() {
                @Override
                public AnAction[] getChildren(@Nullable AnActionEvent e) {
                    return new AnAction[]{clickAction, editConnectionAction, editEnvAction};
                }
            };
        }

        @Override
        public boolean isNavigateAction() {
            return true;
        }
    }

    @AllArgsConstructor
    static class ResourceConnectionNavigationHandler implements GutterIconNavigationHandler<PsiJavaToken> {
        private Connection<AzResource, ?> connection;

        @Override
        public void navigate(@Nonnull final MouseEvent e, @Nonnull final PsiJavaToken token) {
            final Predicate<DefaultMutableTreeNode> rootPredict = node -> node.getUserObject() instanceof AzureFacetRootNode;
            final Predicate<DefaultMutableTreeNode> connectionsPredict = node -> node.getUserObject() instanceof ConnectionsNode;
            final Predicate<DefaultMutableTreeNode> connectionPredict = node -> node.getUserObject() instanceof ConnectionNode
                    && Objects.equals(((ConnectionNode) node.getUserObject()).getValue(), connection);
            final Predicate<DefaultMutableTreeNode> resourcePredict = node -> node.getUserObject() instanceof ResourceNode
                    && Objects.equals(((ResourceNode) node.getUserObject()).getValue().getValue(), connection.getResource().getData());
            AbstractAzureFacetNode.focusNode(token.getProject(), resourcePredict, connectionPredict, connectionsPredict, rootPredict);
        }
    }
}
