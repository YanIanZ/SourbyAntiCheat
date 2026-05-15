package dev.yanianz.sourbyanticheat.manager.init.start;

import dev.yanianz.sourbyanticheat.platform.api.command.CommandService;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;

public record CommandRegister(CommandService service) implements StartableInitable {

    @Override
    public void start() {
        try {
            if (service != null) {
                service.registerCommands();
            }
        } catch (Throwable t) {
            // This is the ultimate safety net. If command registration fails, Grim keeps running.
            LogUtil.error("Failed to register commands! SAC will run without command support.", t);
        }
    }
}
