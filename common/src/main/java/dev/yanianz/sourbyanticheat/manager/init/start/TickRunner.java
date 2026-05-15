package dev.yanianz.sourbyanticheat.manager.init.start;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.platform.api.Platform;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;

public class TickRunner implements StartableInitable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        if (SacAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(SacAPI.INSTANCE.getGrimPlugin(), () -> {
                SacAPI.INSTANCE.getTickManager().tickSync();
                SacAPI.INSTANCE.getTickManager().tickAsync();
            }, 1, 1);
        } else {
            SacAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().runAtFixedRate(SacAPI.INSTANCE.getGrimPlugin(), () -> SacAPI.INSTANCE.getTickManager().tickSync(), 0, 1);
            SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(SacAPI.INSTANCE.getGrimPlugin(), () -> SacAPI.INSTANCE.getTickManager().tickAsync(), 0, 1);
        }
    }
}
