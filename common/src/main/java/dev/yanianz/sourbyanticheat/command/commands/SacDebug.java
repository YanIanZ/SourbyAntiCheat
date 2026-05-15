package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SacDebug implements BuildableCommand {

    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("sac", "sac");

        // Register "debug" subcommand
        Command.Builder<Sender> debugCommand = grimCommand
                .literal("debug", Description.of("Toggle debug output for a player"))
                .permission("sac.debug")
                .optional("target", adapter.singlePlayerSelectorParser())
                .handler(this::handleDebug);

        // Register "consoledebug" subcommand
        Command.Builder<Sender> consoleDebugCommand = grimCommand
                .literal("consoledebug", Description.of("Toggle console debug output for a player"))
                .permission("sac.consoledebug")
                .required("target", adapter.singlePlayerSelectorParser())
                .handler(this::handleConsoleDebug);

        // Register command
        commandManager.command(debugCommand);
        commandManager.command(consoleDebugCommand);
    }

    private void handleDebug(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector playerSelector = context.getOrDefault("target", null);

        SacPlayer targetSacPlayer = parseTarget(sender, playerSelector == null ? sender : playerSelector.getSinglePlayer());
        if (targetSacPlayer == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));
            return;
        }

        if (sender.isConsole()) {
            targetSacPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        } else if (sender.isPlayer()) {
            SacPlayer senderSacPlayer = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
            if (senderSacPlayer == null) {
                sender.sendMessage(MessageUtil.getParsedComponent(sender, "sender-not-found", "%prefix% &cYou cannot be exempt to use this command!"));
                return;
            }
            targetSacPlayer.checkManager.getDebugHandler().toggleListener(senderSacPlayer);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender,
                    "run-as-player-or-console",
                    "%prefix% &cThis command can only be used by players or the console!")
            );
        }
    }

    private void handleConsoleDebug(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector targetName = context.getOrDefault("target", null);

        SacPlayer grimPlayer = parseTarget(sender, targetName.getSinglePlayer());
        if (grimPlayer == null) return;

        boolean isOutput = grimPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        String playerName = grimPlayer.user.getProfile().getName(); // Use user profile for name

        Component message = Component.text()
                .append(Component.text("Console output for ", NamedTextColor.GRAY))
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text(" is now ", NamedTextColor.GRAY))
                .append(Component.text(isOutput ? "enabled" : "disabled", NamedTextColor.WHITE))
                .build();

        sender.sendMessage(message);
    }

    private @Nullable SacPlayer parseTarget(@NotNull Sender sender, @Nullable Sender t) {
        if (sender.isConsole() && t == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "console-specify-target", "%prefix% &cYou must specify a target as the console!"));
            return null;
        }
        Sender target = t == null ? sender : t;

        SacPlayer grimPlayer = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getUniqueId());
        if (grimPlayer == null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(sender.getPlatformPlayer().getNative());
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));

            if (user == null) {
                sender.sendMessage(Component.text("Unknown PacketEvents user", NamedTextColor.RED));
            } else {
                boolean isExempt = SacAPI.INSTANCE.getPlayerDataManager().shouldCheck(user);
                if (!isExempt) {
                    sender.sendMessage(Component.text("User connection state: " + user.getConnectionState(), NamedTextColor.RED));
                }
            }
        }

        return grimPlayer;
    }
}
