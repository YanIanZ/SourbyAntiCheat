package dev.yanianz.sourbyanticheat;

import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.events.GrimReloadEvent;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import dev.yanianz.sourbyanticheat.api.SacAbstractAPI;
import dev.yanianz.sourbyanticheat.manager.config.ConfigManagerFileImpl;
import dev.yanianz.sourbyanticheat.manager.init.start.StartableInitable;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import dev.yanianz.sourbyanticheat.utils.common.ConfigReloadObserver;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

//This is used for grim's external API. It has its own class just for organization.

public class SacExternalAPI implements GrimAbstractAPI, SacAbstractAPI, ConfigReloadObserver, StartableInitable {

    // Holder class — SacExternalAPI is constructed inside SacAPI's ctor,
    // so a plain static-final would see a null SacAPI.INSTANCE. Holder
    // init runs on first fire, after SacAPI is fully built.
    private static final class Channels {
        static final GrimReloadEvent.Channel RELOAD = SacAPI.INSTANCE.getEventBus().get(GrimReloadEvent.class);
    }

    private final SacAPI api;
    @Getter
    private final Map<String, Function<GrimUser, String>> variableReplacements = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, String> staticReplacements = new ConcurrentHashMap<>();
    private final Map<String, Function<Object, Object>> functions = new ConcurrentHashMap<>();
    private final ConfigManagerFileImpl configManagerFile = new ConfigManagerFileImpl();
    private ConfigManager configManager = null;
    private boolean started = false;

    public SacExternalAPI(SacAPI api) {
        this.api = api;
    }

    @Override
    public @NotNull EventBus getEventBus() {
        return api.getEventBus();
    }

    @Override
    public @Nullable GrimUser getGrimUser(Player player) {
        return getGrimUser(player.getUniqueId());
    }

    @Override
    public @Nullable GrimUser getGrimUser(UUID uuid) {
        return api.getPlayerDataManager().getPlayer(uuid);
    }

    @Override
    public void registerVariable(String string, Function<GrimUser, String> replacement) {
        if (replacement == null) {
            variableReplacements.remove(string);
        } else {
            variableReplacements.put(string, replacement);
        }
    }

    @Override
    public void registerVariable(String variable, String replacement) {
        if (replacement == null) {
            staticReplacements.remove(variable);
        } else {
            staticReplacements.put(variable, replacement);
        }
    }

    @Override
    public String getGrimVersion() {
        return getSacVersion();
    }

    public String getSacVersion() {
        return api.getGrimPlugin().getDescription().getVersion();
    }

    @Override
    public void registerFunction(String key, Function<Object, Object> function) {
        if (function == null) {
            functions.remove(key);
        } else {
            functions.put(key, function);
        }
    }

    @Override
    public Function<Object, Object> getFunction(String key) {
        return functions.get(key);
    }

    @Override
    public AlertManager getAlertManager() {
        return SacAPI.INSTANCE.getAlertManager();
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public boolean hasStarted() {
        return started;
    }

    @Override
    public int getCurrentTick() {
        return SacAPI.INSTANCE.getTickManager().currentTick;
    }

    @Override
    public @NotNull GrimPlugin getGrimPlugin(@NotNull Object o) {
        return this.api.getExtensionManager().getPlugin(o);
    }

    @Override
    public @NotNull BackendRegistry getBackendRegistry() {
        return api.getBackendRegistry();
    }

    // on load, load the config & register the service
    public void load() {
        reload(configManagerFile);
        api.getLoader().registerAPIService();
    }

    // handles any config loading that's needed to be done after load
    @Override
    public void start() {
        started = true;
        try {
            SacAPI.INSTANCE.getConfigManager().start();
        } catch (Exception e) {
            LogUtil.error("Failed to start config manager.", e);
        }
    }

    @Override
    public void reload(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(SacAPI.INSTANCE.getGrimPlugin(),
                    () -> successfulReload(config));
        } else {
            successfulReload(config);
        }
    }

    @Override
    public CompletableFuture<Boolean> reloadAsync(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(SacAPI.INSTANCE.getGrimPlugin(),
                    () -> future.complete(successfulReload(config)));
            return future;
        }
        return CompletableFuture.completedFuture(successfulReload(config));
    }

    private boolean successfulReload(ConfigManager config) {
        try {
            config.reload();
            SacAPI.INSTANCE.getConfigManager().load(config);
            if (started) SacAPI.INSTANCE.getConfigManager().start();
            onReload(config);
            if (started)
                SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(SacAPI.INSTANCE.getGrimPlugin(),
                        () -> Channels.RELOAD.fire(true));
            return true;
        } catch (Exception e) {
            LogUtil.error("Failed to reload config", e);
        }
        if (started)
            SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(SacAPI.INSTANCE.getGrimPlugin(),
                    () -> Channels.RELOAD.fire(false));
        return false;
    }

    @Override
    public void onReload(ConfigManager newConfig) {
        if (newConfig == null) {
            LogUtil.warn("ConfigManager not set. Using default config file manager.");
            configManager = configManagerFile;
        } else {
            configManager = newConfig;
        }
        // Update variables
        updateVariables();
        // Restart
        SacAPI.INSTANCE.getAlertManager().reload(configManager);
        SacAPI.INSTANCE.getDiscordManager().reload();
        SacAPI.INSTANCE.getSpectateManager().reload();
        // First-load guard: load() calls reload() before start() runs, so this fires once with started=false before the datastore exists. Subsequent /grim reload calls see started=true and proceed (including disabled→enabled flips — DataStoreLifecycle.reload() re-evaluates builder.enabled() each time).
        if (!started) return;
        // Hot-reload picks up backend swaps + routing + connection-pool edits without a server restart. Drains in-flight writes for shutdown-drain-timeout-ms then drops; brief mid-reload unavailability is the tradeoff.
        if (SacAPI.INSTANCE.getDataStoreLifecycle() != null) {
            SacAPI.INSTANCE.getDataStoreLifecycle().reload();
        }
        // Reload checks for all players
        for (SacPlayer player : SacAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.runSafely(() -> player.reload(configManager));
        }
    }

    private void updateVariables() {
        variableReplacements.putIfAbsent("%player%", GrimUser::getName);
        variableReplacements.putIfAbsent("%uuid%", user -> user.getUniqueId().toString());
        variableReplacements.putIfAbsent("%ping%", user -> user.getTransactionPing() + "");
        variableReplacements.putIfAbsent("%brand%", GrimUser::getBrand);
        variableReplacements.putIfAbsent("%h_sensitivity%", user -> ((int) Math.round(user.getHorizontalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%v_sensitivity%", user -> ((int) Math.round(user.getVerticalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%fast_math%", user -> !user.isVanillaMath() + "");
        variableReplacements.putIfAbsent("%tps%", user -> String.format("%.2f", SacAPI.INSTANCE.getPlatformServer().getTPS()));
        variableReplacements.putIfAbsent("%version%", GrimUser::getVersionName);
        // static variables
        staticReplacements.put("%prefix%", MessageUtil.translateAlternateColorCodes('&', SacAPI.INSTANCE.getConfigManager().getPrefix()));
        staticReplacements.putIfAbsent("%sac_version%", getSacVersion());
    }
}
