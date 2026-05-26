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
            Class<?> hubClass = Class.forName("dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu.HubMenu");
            org.bukkit.entity.Player player = getBukkitPlayer(sender);
            if (player == null) {
                sender.sendMessage(Component.text("Could not get player instance.", SacColors.RED));
                return;
            }
            Object hub = hubClass.getDeclaredConstructor().newInstance();
            hubClass.getMethod("open", org.bukkit.entity.Player.class).invoke(hub, player);
        } catch (ClassNotFoundException e) {
            sender.sendMessage(Component.text("GUI not available on this platform.", SacColors.RED));
        } catch (Throwable e) {
            // Reflection wraps the real failure in InvocationTargetException (null
            // message). Unwrap so the cause is visible instead of "null".
            Throwable cause = (e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null)
                    ? e.getCause() : e;
            sender.sendMessage(Component.text("Error opening GUI: " + cause, SacColors.RED));
            dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil.error("Failed to open SAC GUI", cause);
        }
    }

    private static org.bukkit.entity.Player getBukkitPlayer(Sender sender) {
        try {
            return (org.bukkit.entity.Player) sender.getPlatformPlayer().getNative();
        } catch (Exception e) {
            return null;
        }
    }
}
