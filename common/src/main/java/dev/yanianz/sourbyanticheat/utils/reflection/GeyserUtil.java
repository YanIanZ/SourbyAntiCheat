package dev.yanianz.sourbyanticheat.utils.reflection;

import lombok.experimental.UtilityClass;
import java.util.UUID;

@UtilityClass
public class GeyserUtil {
    private static final boolean floodgateAvailable = ReflectionUtils.hasClass("org.geysermc.floodgate.api.FloodgateApi");
    private static final boolean geyserAvailable = ReflectionUtils.hasClass("org.geysermc.api.Geyser");

    public static boolean isAvailable() {
        return floodgateAvailable || geyserAvailable;
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (floodgateAvailable) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object api = apiClass.getMethod("getInstance").invoke(null);
                return (boolean) apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, uuid);
            } catch (Exception ignored) {}
        }
        if (geyserAvailable) {
            try {
                Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser");
                Object api = geyserClass.getMethod("api").invoke(null);
                return (boolean) api.getClass().getMethod("isBedrockPlayer", UUID.class).invoke(api, uuid);
            } catch (Exception ignored) {}
        }
        return false;
    }
}
