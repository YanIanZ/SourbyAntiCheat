package dev.yanianz.sourbyanticheat;

import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.internal.plugin.resolver.GrimExtensionManager;
import ac.grim.grimac.internal.event.OptimizedEventBus;
import ac.grim.grimac.internal.storage.backend.BackendRegistryImpl;
import ac.grim.grimac.internal.storage.backend.memory.InMemoryBackendProvider;
import ac.grim.grimac.internal.storage.backend.mongo.MongoBackendProvider;
import ac.grim.grimac.internal.storage.backend.mysql.MysqlBackendProvider;
import ac.grim.grimac.internal.storage.backend.postgres.PostgresBackendProvider;
import ac.grim.grimac.internal.storage.backend.redis.RedisBackendProvider;
import ac.grim.grimac.internal.storage.backend.sqlite.SqliteBackendProvider;
import dev.yanianz.sourbyanticheat.manager.AlertManagerImpl;
import dev.yanianz.sourbyanticheat.manager.AutoPunishment;
import dev.yanianz.sourbyanticheat.manager.DiscordManager;
import dev.yanianz.sourbyanticheat.manager.WavePunishment;
import dev.yanianz.sourbyanticheat.manager.InitManager;
import dev.yanianz.sourbyanticheat.manager.SpectateManager;
import dev.yanianz.sourbyanticheat.manager.TickManager;
import dev.yanianz.sourbyanticheat.manager.config.BaseConfigManager;
import dev.yanianz.sourbyanticheat.manager.datastore.DataStoreLifecycle;
import dev.yanianz.sourbyanticheat.manager.init.Initable;
import dev.yanianz.sourbyanticheat.platform.api.Platform;
import dev.yanianz.sourbyanticheat.platform.api.PlatformLoader;
import dev.yanianz.sourbyanticheat.platform.api.PlatformServer;
import dev.yanianz.sourbyanticheat.platform.api.command.CommandService;
import dev.yanianz.sourbyanticheat.platform.api.manager.ItemResetHandler;
import dev.yanianz.sourbyanticheat.platform.api.manager.MessagePlaceHolderManager;
import dev.yanianz.sourbyanticheat.platform.api.manager.PermissionRegistrationManager;
import dev.yanianz.sourbyanticheat.platform.api.manager.PlatformPluginManager;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayerFactory;
import dev.yanianz.sourbyanticheat.platform.api.proxy.AuditLogger;
import dev.yanianz.sourbyanticheat.platform.api.proxy.GlobalPlayerStore;
import dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyMessenger;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.PlatformScheduler;
import dev.yanianz.sourbyanticheat.platform.api.sender.SenderFactory;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.PlayerDataManager;
import dev.yanianz.sourbyanticheat.utils.common.arguments.CommonGrimArguments;
import dev.yanianz.sourbyanticheat.utils.reflection.ReflectionUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;


@Getter
public final class SacAPI {
    public static final SacAPI INSTANCE = new SacAPI();

    @Getter
    private final Platform platform = detectPlatform();
    private final BaseConfigManager configManager;
    private final AlertManagerImpl alertManager;
    private final SpectateManager spectateManager;
    private final DiscordManager discordManager;
    private final PlayerDataManager playerDataManager;
    private final TickManager tickManager;
    private final GrimExtensionManager extensionManager;
    private final EventBus eventBus;
    private final SacExternalAPI externalAPI;
    private DataStoreLifecycle dataStoreLifecycle;
    private final BackendRegistry backendRegistry = buildBackendRegistry();
    private PlatformLoader loader;
    @Getter
    private InitManager initManager;
    private ProxyMessenger proxyMessenger;
    private GlobalPlayerStore globalPlayerStore;
    private AuditLogger auditLogger;
    private boolean initialized = false;
    private final long startTime = System.currentTimeMillis();

    private SacAPI() {
        this.configManager = new BaseConfigManager();
        this.alertManager = new AlertManagerImpl();
        this.spectateManager = new SpectateManager();
        this.discordManager = new DiscordManager();
        this.playerDataManager = new PlayerDataManager();
        this.tickManager = new TickManager();
        this.extensionManager = new GrimExtensionManager();
        this.eventBus = new OptimizedEventBus(extensionManager);
        this.externalAPI = new SacExternalAPI(this);
    }

    // the order matters
    private static Platform detectPlatform() {
        Platform override = CommonGrimArguments.PLATFORM_OVERRIDE.value();
        if (override != null) return override;
        if (ReflectionUtils.hasClass("io.papermc.paper.threadedregions.RegionizedServer")) return Platform.FOLIA;
        if (ReflectionUtils.hasClass("org.bukkit.Bukkit")) return Platform.BUKKIT;
                throw new IllegalStateException("Unknown platform!");
    }

    public void load(PlatformLoader platformLoader, Initable... platformSpecificInitables) {
        this.loader = platformLoader;
        this.proxyMessenger = platformLoader.getProxyMessenger();
        this.globalPlayerStore = platformLoader.getGlobalPlayerStore();
        this.auditLogger = platformLoader.getAuditLogger();
        this.dataStoreLifecycle = new DataStoreLifecycle(getGrimPlugin(), backendRegistry);
        this.initManager = new InitManager(loader.getPacketEvents(), platformSpecificInitables);
        this.initManager.load();
        this.initialized = true;
    }

    private static BackendRegistry buildBackendRegistry() {
        BackendRegistryImpl registry = new BackendRegistryImpl();
        registry.register(new SqliteBackendProvider());
        registry.register(new InMemoryBackendProvider());
        registry.register(new MysqlBackendProvider());
        registry.register(new PostgresBackendProvider());
        registry.register(new MongoBackendProvider());
        registry.register(new RedisBackendProvider());
        return registry;
    }

    public void start() {
        checkInitialized();
        if (configManager.getConfig().getBooleanElse("spartanapi.cross-check", false)) {
            SpartanCrossCheck.init(true);
        }
        AutoPunishment.init();
        WavePunishment.init();
        initManager.start();
    }

    public void stop() {
        checkInitialized();
        initManager.stop();
    }

    public PlatformScheduler getScheduler() {
        return loader.getScheduler();
    }

    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return loader.getPlatformPlayerFactory();
    }

    public GrimPlugin getGrimPlugin() {
        return loader.getPlugin();
    }

    public GrimPlugin getSacPlugin() {
        return loader.getPlugin();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getSacVersion() {
        return externalAPI.getSacVersion();
    }

    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public SenderFactory<?> getSenderFactory() {
        return loader.getSenderFactory();
    }

    public ItemResetHandler getItemResetHandler() {
        return loader.getItemResetHandler();
    }

    public PlatformPluginManager getPluginManager() {
        return loader.getPluginManager();
    }

    public PlatformServer getPlatformServer() {
        return loader.getPlatformServer();
    }

    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return loader.getMessagePlaceHolderManager();
    }

    public CommandService getCommandService() {
        return loader.getCommandService();
    }

    public void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SacAPI has not been initialized!");
        }
    }

    public PermissionRegistrationManager getPermissionManager() {
        return loader.getPermissionManager();
    }

    public GrimExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public ProxyMessenger getProxyMessenger() { return proxyMessenger; }
    public GlobalPlayerStore getGlobalPlayerStore() { return globalPlayerStore; }
    public AuditLogger getAuditLogger() { return auditLogger; }
}
