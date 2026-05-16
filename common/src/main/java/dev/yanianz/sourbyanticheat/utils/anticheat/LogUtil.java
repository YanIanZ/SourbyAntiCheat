package dev.yanianz.sourbyanticheat.utils.anticheat;

import dev.yanianz.sourbyanticheat.SacAPI;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

@UtilityClass
public class LogUtil {
    private static final String PREFIX = "[SAC] ";

    public void info(final String info) {
        getLogger().info(PREFIX + info);
    }

    public void warn(final String warn) {
        getLogger().warning(PREFIX + warn);
    }

    public void warn(final String description, final Throwable throwable) {
        Logger logger = getLogger();
        if (logger != null) {
            logger.warning(PREFIX + description + ": " + getStackTrace(throwable));
        } else {
            throwable.printStackTrace();
        }
    }

    public void error(final String error) {
        getLogger().severe(PREFIX + error);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void error(final String description, final Throwable throwable) {
        Logger logger = getLogger();
        if (logger != null) {
            logger.severe(PREFIX + description + ": " + getStackTrace(throwable));
        } else {
            throwable.printStackTrace();
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void error(final Throwable throwable) {
        Logger logger = getLogger();
        if (logger != null) {
            logger.severe(PREFIX + getStackTrace(throwable));
        } else {
            throwable.printStackTrace();
        }
    }

    public Logger getLogger() {
        return SacAPI.INSTANCE.getGrimPlugin().getLogger();
    }

    public void console(final String info) {
        SacAPI.INSTANCE.getPlatformServer().getConsoleSender().sendMessage(MessageUtil.translateAlternateColorCodes('&', info));
    }

    public void console(final Component info) {
        SacAPI.INSTANCE.getPlatformServer().getConsoleSender().sendMessage(info);
    }

    private static String getStackTrace(Throwable throwable) {
        String message = throwable.getMessage();
        try (StringWriter sw = new StringWriter()) {
            try (PrintWriter pw = new PrintWriter(sw)) {
                throwable.printStackTrace(pw);
                message = sw.toString();
            }
        } catch (Exception ignored) {
        }
        return message;
    }

}
