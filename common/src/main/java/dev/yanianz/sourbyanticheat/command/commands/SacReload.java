package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class SacReload implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("reload")
                        .permission("sac.reload")
                        .handler(this::handleReload)
        );
    }

    private void handleReload(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        // reload config
        sender.sendMessage(MessageUtil.getParsedComponent(sender, "reloading", "%prefix% &7Reloading config..."));

        SacAPI.INSTANCE.getExternalAPI().reloadAsync().exceptionally(throwable -> false)
                .thenAccept(bool -> {
                    Component message = bool
                            ? MessageUtil.getParsedComponent(sender, "reloaded", "%prefix% &fConfig has been reloaded.")
                            : MessageUtil.getParsedComponent(sender, "reload-failed", "%prefix% &cFailed to reload config.");
                    sender.sendMessage(message);
                });
    }
}
