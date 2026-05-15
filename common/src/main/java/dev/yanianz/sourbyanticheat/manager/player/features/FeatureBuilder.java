package dev.yanianz.sourbyanticheat.manager.player.features;

import dev.yanianz.sourbyanticheat.manager.player.features.types.SacFeature;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import com.google.common.collect.ImmutableMap;

import java.util.regex.Pattern;

public class FeatureBuilder {

    private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9_]{1,64}");
    private final ImmutableMap.Builder<String, SacFeature> mapBuilder = ImmutableMap.builder();

    public <T extends SacFeature> void register(T feature) {
        if (!VALID.matcher(feature.getName()).matches()) {
            LogUtil.error("Invalid feature name: " + feature.getName());
            return;
        }
        mapBuilder.put(feature.getName(), feature);
    }

    public ImmutableMap<String, SacFeature> buildMap() {
        return mapBuilder.build();
    }

}
