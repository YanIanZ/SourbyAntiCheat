package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class SacExempt implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("exempt", Description.of("Toggle player exemption from checks"))
                        .permission("sac.exempt")
                        .required("target", adapter.singlePlayerSelectorParser())
                        .handler(this::handleExempt)
        );
    }

    private void handleExempt(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        PlatformPlayer tp = target.getSinglePlayer().getPlatformPlayer();
        if (tp == null) return;

        SacPlayer sp = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(tp.getUniqueId());
        if (sp == null) {
            sender.sendMessage(Component.text("  " + SacColors.CROSS + " Player not tracked.", SacColors.RED));
            return;
        }

        boolean currently = sp.disableGrim;
        sp.disableGrim = !currently;

        sender.sendMessage(Component.text()
            .append(Component.text("  " + (sp.disableGrim ? SacColors.STAR : SacColors.DOT) + " ",
                sp.disableGrim ? SacColors.HIGHLIGHT : SacColors.GREEN))
            .append(Component.text(tp.getName(), SacColors.ACCENT))
            .append(Component.text(" " + SacColors.ARROW_RIGHT + " ", SacColors.MUTED))
            .append(Component.text(sp.disableGrim ? "EXEMPT" : "CHECKED",
                sp.disableGrim ? SacColors.HIGHLIGHT : SacColors.GREEN))
            .build());

        SacAPI.INSTANCE.getAuditLogger().logAction(
            sender.getUniqueId(), sender.getName(),
            sp.disableGrim ? "EXEMPT_ADD" : "EXEMPT_REMOVE",
            tp.getName(), "Toggled by " + sender.getName(), true);
    }
}
