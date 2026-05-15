package dev.yanianz.sourbyanticheat.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class ExperimentalChecksFeature implements SacFeature {

    @Override
    public String getName() {
        return "ExperimentalChecks";
    }

    @Override
    public void setState(SacPlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExperimentalChecks(true);
            case DISABLED -> player.setExperimentalChecks(false);
            default -> player.setExperimentalChecks(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(SacPlayer player) {
        return player.isExperimentalChecks();
    }

    @Override
    public boolean isEnabledInConfig(SacPlayer player, ConfigManager config) {
        return config.getBooleanElse("experimental-checks", false);
    }

}
