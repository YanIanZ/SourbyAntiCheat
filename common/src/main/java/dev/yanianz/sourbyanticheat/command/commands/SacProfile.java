package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SacProfile implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("profile")
                        .permission("sac.profile")
                        .required("target", adapter.singlePlayerSelectorParser())
                        .handler(this::handleProfile)
        );
    }

    private void handleProfile(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");

        PlatformPlayer targetPlatformPlayer = target.getSinglePlayer().getPlatformPlayer();
        if (Objects.requireNonNull(targetPlatformPlayer).isExternalPlayer()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender,"player-not-this-server", "%prefix% &cThis player isn't on this server!"));
            return;
        }

        SacPlayer grimPlayer = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(targetPlatformPlayer.getUniqueId());
        if (grimPlayer == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));
            return;
        }

        for (String message : SacAPI.INSTANCE.getConfigManager().getConfig().getStringList("profile")) {
            final Component component = MessageUtil.miniMessage(message);
            sender.sendMessage(MessageUtil.replacePlaceholders(grimPlayer, component));
        }

        if (SpartanCrossCheck.isAvailable()) {
            var stats = SpartanCrossCheck.getStats(grimPlayer.uuid);
            if (stats.agreements > 0 || stats.disagreements > 0) {
                sender.sendMessage(Component.text()
                    .append(Component.text("  " + SacColors.DIAMOND + " ", SacColors.ACCENT2))
                    .append(Component.text("Spartan  ", SacColors.GRAY))
                    .append(Component.text(stats.agreements + " " + SacColors.CHECKMARK, SacColors.GREEN))
                    .append(Component.text("  " + stats.disagreements + " " + SacColors.CROSS, SacColors.RED))
                    .append(Component.text("  (", SacColors.MUTED))
                    .append(Component.text(String.format("%.0f%%", stats.agreementRate() * 100), SacColors.CYAN))
                    .append(Component.text(")", SacColors.MUTED))
                    .build());
            }
        }
    }
}
