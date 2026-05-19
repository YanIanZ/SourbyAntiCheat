package dev.yanianz.sourbyanticheat.utils.viaversion;

import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import dev.yanianz.sourbyanticheat.utils.reflection.ReflectionUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ViaVersionUtil {
    public static final boolean isAvailable = ReflectionUtils.hasClass("com.viaversion.viaversion.api.Via");
    public static final boolean hasViaBackwards = ViaVersionUtil.isAvailable && ReflectionUtils.hasClass("com.viaversion.viabackwards.ViaBackwards");

    static {
        if (!isAvailable && ReflectionUtils.hasClass("us.myles.ViaVersion.api.Via")) {
            LogUtil.error("Using unsupported ViaVersion 4.0 API, update ViaVersion to 5.0");
        }
    }

    public static void injectHooks() {
        if (isAvailable) ViaVersionHooks.load();
    }

    public static final int MIN_PROTOCOL = 47;
    public static final int MAX_PROTOCOL = 767;

    public static boolean isSupportedVersion(int protocolVersion) {
        return protocolVersion >= MIN_PROTOCOL && protocolVersion <= MAX_PROTOCOL;
    }

    public static boolean isNativeVersion(int protocolVersion) {
        return protocolVersion >= 767;
    }

    /**
     * Returns true when the player connects through ViaVersion/ViaBackwards on a
     * non-native protocol — i.e. their movement/packets are version-translated.
     */
    public static boolean isCrossVersion(dev.yanianz.sourbyanticheat.player.SacPlayer player) {
        return !isNativeVersion(player.getClientVersion().getProtocolVersion());
    }
}
