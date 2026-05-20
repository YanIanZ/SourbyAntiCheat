package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.PlatformPlugin;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import dev.yanianz.sourbyanticheat.utils.common.PropertiesUtil;
import dev.yanianz.sourbyanticheat.utils.reflection.ReflectionUtils;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Properties;

public class SacDump implements BuildableCommand {

    private static final boolean PAPER = ReflectionUtils.hasClass("com.destroystokyo.paper.PaperConfig")
            || ReflectionUtils.hasClass("io.papermc.paper.configuration.Configuration");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private String link = null;
    private long linkTime = 0;
    private static final long LINK_TTL_MS = 300_000;

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("dump", Description.of("Generate a debug dump"))
                        .permission("sac.dump")
                        .handler(this::handleDump)
        );
    }

    private void handleDump(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        if (link != null && System.currentTimeMillis() - linkTime < LINK_TTL_MS) {
            sender.sendMessage(MessageUtil.miniMessage(SacAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("upload-log", "%prefix% &fUploaded debug to: %url%")
                    .replace("%url%", link)));
            return;
        }
        link = null;
        SacLog.sendLogAsync(sender, generateDump(), s -> { link = s; linkTime = System.currentTimeMillis(); }, "text/yaml");
    }

    public static JsonObject getDumpInfo() {
        JsonObject base = new JsonObject();
        base.addProperty("type", "dump");
        base.addProperty("timestamp", System.currentTimeMillis());
        // versions
        JsonObject versions = new JsonObject();
        base.add("versions", versions);
        versions.addProperty("sac", SacAPI.INSTANCE.getExternalAPI().getSacVersion());
        versions.addProperty("packetevents", PacketEvents.getAPI().getVersion().toString());
        versions.addProperty("server", PacketEvents.getAPI().getServerManager().getVersion().getReleaseName());
        versions.addProperty("implementation", SacAPI.INSTANCE.getPlatformServer().getPlatformImplementationString());
        // state of different properties
        JsonObject states = new JsonObject();
        base.add("states", states);
        if (SacAPI.INSTANCE.isInitialized()) states.addProperty("platform", SacAPI.INSTANCE.getPlatform().toString());
        if (ViaVersionUtil.isAvailable) states.addProperty("has_viaversion", true);
        if (PAPER) states.addProperty("has_paper", true);
        // include some relevant settings if not default
        JsonObject settings = new JsonObject();
        if (SacAPI.INSTANCE.getAlertManager().hasConsoleVerboseEnabled()) settings.addProperty("console_verbose", true);
        if (!SacAPI.INSTANCE.getAlertManager().hasConsoleAlertsEnabled()) settings.addProperty("console_alerts", false);
        if (settings.size() > 0) states.add("settings", settings);
        // system
        JsonObject system = new JsonObject();
        base.add("system", system);
        system.addProperty("os_name", System.getProperty("os.name"));
        system.addProperty("java_version", System.getProperty("java.version"));
        system.addProperty("user_language", System.getProperty("user.language"));
        // build
        base.add("build", getBuildInfo());
        // plugins
        JsonArray plugins = new JsonArray();
        base.add("plugins", plugins);
        for (PlatformPlugin plugin : SacAPI.INSTANCE.getPluginManager().getPlugins()) {
            JsonObject pluginJson = new JsonObject();
            pluginJson.addProperty("enabled", plugin.isEnabled());
            pluginJson.addProperty("name", plugin.getName());
            pluginJson.addProperty("version", plugin.getVersion());
            plugins.add(pluginJson);
        }
        return base;
    }

    private static JsonObject getBuildInfo() {
        JsonObject object = new JsonObject();
        try {
            Properties properties = PropertiesUtil.readProperties(SacAPI.INSTANCE.getClass(), "sac.properties");
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                object.addProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        } catch (Exception ignored) {}
        return object;
    }

    /**
     * Generates a diagnostic dump in JSON format that contains various metadata
     * about the system, platform, and plugins. This dump is primarily used for
     * debugging and finding potential issues with the environment.
     * @return A JSON-formatted string containing the diagnostic dump.
     */
    private String generateDump() {
        JsonObject base = getDumpInfo();
        return gson.toJson(base);
    }
}
