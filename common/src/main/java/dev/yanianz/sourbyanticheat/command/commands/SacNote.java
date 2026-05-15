package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SacNote implements BuildableCommand {

    private static final Map<UUID, String> notes = new ConcurrentHashMap<>();

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("note", Description.of("Add/view staff note for a player"))
                        .permission("sac.admin")
                        .required("target", adapter.singlePlayerSelectorParser())
                        .required("message", StringParser.stringParser())
                        .handler(this::handleNote)
        );
    }

    private void handleNote(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        String message = context.get("message");
        PlatformPlayer tp = target.getSinglePlayer().getPlatformPlayer();
        if (tp == null) return;

        String existing = notes.get(tp.getUniqueId());
        String entry = "[" + sender.getName() + "] " + message;
        notes.put(tp.getUniqueId(), existing == null ? entry : existing + " | " + entry);

        sender.sendMessage(Component.text()
            .append(Component.text("  " + SacColors.CHECKMARK + " Note for ", SacColors.GREEN))
            .append(Component.text(tp.getName(), SacColors.ACCENT))
            .append(Component.text(" " + SacColors.DASH + " ", SacColors.MUTED))
            .append(Component.text(message, SacColors.WHITE))
            .build());

        SacAPI.INSTANCE.getAuditLogger().logAction(
            sender.getUniqueId(), sender.getName(), "NOTE_ADD", tp.getName(), message, true);
    }
}
