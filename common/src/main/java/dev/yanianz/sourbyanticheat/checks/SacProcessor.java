package dev.yanianz.sourbyanticheat.checks;

import dev.yanianz.sourbyanticheat.SacAPI;
import ac.grim.grimac.api.AbstractProcessor;
import ac.grim.grimac.api.config.ConfigReloadable;
import dev.yanianz.sourbyanticheat.utils.common.ConfigReloadObserver;

public abstract class SacProcessor implements AbstractProcessor, ConfigReloadable, ConfigReloadObserver {

    // Not everything has to be a check for it to process packets & be configurable

    @Override
    public void reload() {
        reload(SacAPI.INSTANCE.getConfigManager().getConfig());
    }

}
