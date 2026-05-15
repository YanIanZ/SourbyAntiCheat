package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import dev.yanianz.sourbyanticheat.utils.data.webhook.discord.WebhookMessage;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class SacTestWebhook implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("testwebhook")
                        .permission("sac.testwebhook")
                        .handler(this::handleTestWebhook)
        );
    }

    private void handleTestWebhook(@NotNull CommandContext<Sender> context) {
        if (SacAPI.INSTANCE.getDiscordManager().isDisabled()) {
            context.sender().sendMessage(MessageUtil.miniMessage(SacAPI.INSTANCE.getConfigManager().getWebhookNotEnabled()));
            return;
        }

        WebhookMessage webhookMessage = new WebhookMessage().content(SacAPI.INSTANCE.getConfigManager().getWebhookTestMessage());
        SacAPI.INSTANCE.getDiscordManager().sendWebhookMessage(webhookMessage).whenCompleteAsync(((successful, throwable) -> {
            if (successful == true) {
                context.sender().sendMessage(MessageUtil.miniMessage(SacAPI.INSTANCE.getConfigManager().getWebhookTestSucceeded()));
                return;
            }

            context.sender().sendMessage(MessageUtil.miniMessage(SacAPI.INSTANCE.getConfigManager().getWebhookTestFailed()));

            if (throwable != null) {
                LogUtil.error("Exception caught while sending a Discord webhook test alert", throwable);
            }
        }));
    }
}
