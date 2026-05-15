package dev.yanianz.sourbyanticheat.manager.init.start;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.commands.SacVersion;

public class UpdateChecker implements StartableInitable {
    @Override
    public void start() {
        if (SacAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            SacVersion.checkForUpdatesAsync(SacAPI.INSTANCE.getPlatformServer().getConsoleSender());
        }
    }
}
