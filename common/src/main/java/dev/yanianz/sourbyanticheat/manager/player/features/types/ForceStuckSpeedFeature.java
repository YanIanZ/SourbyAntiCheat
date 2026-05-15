package dev.yanianz.sourbyanticheat.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class ForceStuckSpeedFeature implements SacFeature {

    @Override
    public String getName() {
        return "ForceStuckSpeed";
    }

    @Override
    public void setState(SacPlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setForceStuckSpeed(true);
            case DISABLED -> player.setForceStuckSpeed(false);
            default -> player.setForceStuckSpeed(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(SacPlayer player) {
        return player.isForceStuckSpeed();
    }

    @Override
    public boolean isEnabledInConfig(SacPlayer player, ConfigManager config) {
        return config.getBooleanElse("force-stuck-speed", true);
    }

}
