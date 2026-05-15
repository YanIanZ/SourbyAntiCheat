package dev.yanianz.sourbyanticheat.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public interface SacFeature {
    String getName();

    void setState(SacPlayer player, ConfigManager config, FeatureState state);

    boolean isEnabled(SacPlayer player);

    boolean isEnabledInConfig(SacPlayer player, ConfigManager config);
}
