package dev.yanianz.sourbyanticheat.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class ForceSlowMovementFeature implements SacFeature {

    @Override
    public String getName() {
        return "ForceSlowMovement";
    }

    @Override
    public void setState(SacPlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setForceSlowMovement(true);
            case DISABLED -> player.setForceSlowMovement(false);
            default -> player.setForceSlowMovement(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(SacPlayer player) {
        return player.isForceSlowMovement();
    }

    @Override
    public boolean isEnabledInConfig(SacPlayer player, ConfigManager config) {
        return config.getBooleanElse("force-slow-movement", true);
    }

}
