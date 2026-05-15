package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.manager.AlertManagerImpl;
import dev.yanianz.sourbyanticheat.manager.datastore.PlayerToggleStore;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SacAlerts implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("alerts", Description.of("Toggle alerts for the sender"))
                        .permission("sac.alerts")
                        .handler(this::handleAlerts)
        );
    }

    private void handleAlerts(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            PlatformPlayer p = Objects.requireNonNull(context.sender().getPlatformPlayer());
            AlertManagerImpl am = SacAPI.INSTANCE.getAlertManager();
            boolean newState = !am.hasAlertsEnabled(p);
            am.setAlertsEnabled(p, newState, false);
            SacAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore()
                    .applyUserToggle(p.getUniqueId(), PlayerToggleStore.KEY_ALERTS, newState);
        } else if (sender.isConsole()) {
            SacAPI.INSTANCE.getAlertManager().toggleConsoleAlerts();
        }
    }
}
