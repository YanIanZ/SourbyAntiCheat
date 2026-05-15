package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class SacGUICommand implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("gui", Description.of("Open the SAC control panel"))
                        .permission("sac.admin")
                        .handler(this::handleGUI)
        );
    }

    private void handleGUI(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!sender.isPlayer()) {
            sender.sendMessage(Component.text("GUI only available to players.", SacColors.RED));
            return;
        }
        try {
            Class<?> guiClass = Class.forName("dev.yanianz.sourbyanticheat.platform.bukkit.gui.SacGUI");
            org.bukkit.entity.Player player = getBukkitPlayer(sender);
            if (player == null) {
                sender.sendMessage(Component.text("Could not get player instance.", SacColors.RED));
                return;
            }
            guiClass.getMethod("openMain", org.bukkit.entity.Player.class)
                .invoke(null, player);
        } catch (ClassNotFoundException e) {
            sender.sendMessage(Component.text("GUI not available on this platform.", SacColors.RED));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error opening GUI: " + e.getMessage(), SacColors.RED));
        }
    }

    private static org.bukkit.entity.Player getBukkitPlayer(Sender sender) {
        try {
            var platformPlayer = sender.getPlatformPlayer();
            return (org.bukkit.entity.Player) platformPlayer.getClass()
                .getMethod("getPlayer").invoke(platformPlayer);
        } catch (Exception e) {
            return null;
        }
    }
}
