package dev.yanianz.sourbyanticheat.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class ExemptElytraFeature implements SacFeature {

    @Override
    public String getName() {
        return "ExemptElytra";
    }

    @Override
    public void setState(SacPlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExemptElytra(true);
            case DISABLED -> player.setExemptElytra(false);
            default -> player.setExemptElytra(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(SacPlayer player) {
        return player.isExemptElytra();
    }

    @Override
    public boolean isEnabledInConfig(SacPlayer player, ConfigManager config) {
        return config.getBooleanElse("exempt-elytra", false);
    }

}
