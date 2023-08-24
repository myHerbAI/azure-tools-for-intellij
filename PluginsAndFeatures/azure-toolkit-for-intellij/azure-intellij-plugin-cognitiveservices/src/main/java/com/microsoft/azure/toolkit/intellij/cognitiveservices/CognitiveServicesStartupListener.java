package com.microsoft.azure.toolkit.intellij.cognitiveservices;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.ide.cognitiveservices.CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND;
import static com.microsoft.azure.toolkit.intellij.cognitiveservices.IntelliJCognitiveServicesActionsContributor.TRY_OPENAI;
import static com.microsoft.azure.toolkit.intellij.cognitiveservices.IntelliJCognitiveServicesActionsContributor.TRY_PLAYGROUND;

public class CognitiveServicesStartupListener implements ProjectActivity {
    public static final AzureString TRY_OPENAI_MESSAGE = AzureString.format("%s is supported now. " +
        "You can use Azure OpenAI to build your own \"%s\". " +
        "<a href='https://docs.microsoft.com/en-us/azure/openai/'>learn more</a> about Azure OpenAI.", "Azure OpenAI", "ChatGPT");

    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        final String tryOpenAIId = TRY_OPENAI.getId();
        final String tryPlaygroundId = TRY_PLAYGROUND.getId();
        if (BooleanUtils.isNotTrue(IntellijStore.getInstance().getState().getSuppressedActions().get(tryOpenAIId))) {
            final Action<Project> tryOpenAI = AzureActionManager.getInstance().getAction(TRY_OPENAI).bind(project);
            final Action<Object> dismiss = new Action<>(Action.Id.of("user/common.never_show_again"))
                .withLabel("Don't show again")
                .withHandler((e) -> IntellijStore.getInstance().getState().getSuppressedActions().put(tryOpenAIId, true))
                .withAuthRequired(false);
            AzureMessager.getMessager().info(TRY_OPENAI_MESSAGE, tryOpenAI, dismiss);
        }
        if (BooleanUtils.isNotTrue(IntellijStore.getInstance().getState().getSuppressedActions().get(tryPlaygroundId))) {
            AzureEventBus.once("account.subscription_changed.account", (_a, _b) -> Azure.az(AzureCognitiveServices.class).list().stream()
                .flatMap(m -> m.accounts().list().stream())
                .flatMap(a -> a.deployments().list().stream())
                .filter(d -> d.getModel().isGPTModel())
                .findFirst().ifPresent(d -> {
                    final Action<CognitiveDeployment> tryPlayGround =
                        new Action<>(TRY_PLAYGROUND)
                            .withLabel("Open in AI Playground")
                            .withSource(d)
                            .withHandler((_d, e) -> {
                                AzureActionManager.getInstance().getAction(OPEN_DEPLOYMENT_IN_PLAYGROUND).handle(d, e);
                                IntellijStore.getInstance().getState().getSuppressedActions().put(tryPlaygroundId, true);
                            })
                            .withAuthRequired(true);
                    final Action<Object> dismiss = new Action<>(Action.Id.of("user/common.never_show_again"))
                        .withLabel("Don't show again")
                        .withHandler((e) -> IntellijStore.getInstance().getState().getSuppressedActions().put(tryPlaygroundId, true))
                        .withAuthRequired(false);
                    final AzureString msg = AzureString.format("GPT* model based deployment (%s) detected in your Cognitive Service account (%s). " +
                        "You can try your own \"%s\" in playground.", d.getName(), d.getParent().getName(), "ChatGPT");
                    AzureMessager.getMessager().info(msg, tryPlayGround, dismiss);
                }));
        }
        return null;
    }
}
