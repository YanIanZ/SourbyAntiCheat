package dev.yanianz.sourbyanticheat.platform.bukkit;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.SacExternalAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import dev.yanianz.sourbyanticheat.command.CloudCommandService;
import ac.grim.grimac.internal.platform.bukkit.resolver.BukkitResolverRegistrar;
import dev.yanianz.sourbyanticheat.manager.init.Initable;
import dev.yanianz.sourbyanticheat.manager.init.start.ExemptOnlinePlayersOnReload;
import dev.yanianz.sourbyanticheat.manager.init.start.StartableInitable;
import dev.yanianz.sourbyanticheat.api.SacAbstractAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.profile.Profile;
import dev.yanianz.sourbyanticheat.profile.ProfileConfig;
import dev.yanianz.sourbyanticheat.profile.ProfileRegistry;
import dev.yanianz.sourbyanticheat.profile.ProfileResolver;
import dev.yanianz.sourbyanticheat.profile.ProfileWorldMap;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyTracker;
import dev.yanianz.sourbyanticheat.profile.leniency.handlers.ElytraFireworkBoostHandler;
import dev.yanianz.sourbyanticheat.profile.leniency.handlers.EnderPearlLandHandler;
import dev.yanianz.sourbyanticheat.profile.leniency.handlers.FireballBoostHandler;
import dev.yanianz.sourbyanticheat.profile.leniency.handlers.KitPotionApplyHandler;
import dev.yanianz.sourbyanticheat.profile.leniency.handlers.MLGWaterLandHandler;
import dev.yanianz.sourbyanticheat.profile.leniency.handlers.RodPullHandler;
import dev.yanianz.sourbyanticheat.profile.leniency.handlers.SnowballKbHandler;
import dev.yanianz.sourbyanticheat.netty.SacNettyInjector;
import dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyMessenger;
import dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu.MenuRouter;
import dev.yanianz.sourbyanticheat.platform.api.Platform;
import dev.yanianz.sourbyanticheat.platform.api.PlatformLoader;
import dev.yanianz.sourbyanticheat.platform.api.PlatformServer;
import dev.yanianz.sourbyanticheat.platform.api.command.CommandService;
import dev.yanianz.sourbyanticheat.platform.api.manager.ItemResetHandler;
import dev.yanianz.sourbyanticheat.platform.api.manager.MessagePlaceHolderManager;
import dev.yanianz.sourbyanticheat.platform.api.manager.PlatformPluginManager;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayerFactory;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.PlatformScheduler;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.platform.api.sender.SenderFactory;
import dev.yanianz.sourbyanticheat.platform.bukkit.initables.BukkitBStats;
import dev.yanianz.sourbyanticheat.platform.bukkit.initables.BukkitEventManager;
import dev.yanianz.sourbyanticheat.platform.bukkit.initables.BukkitTickEndEvent;
import dev.yanianz.sourbyanticheat.platform.bukkit.manager.BukkitItemResetHandler;
import dev.yanianz.sourbyanticheat.platform.bukkit.manager.BukkitMessagePlaceHolderManager;
import dev.yanianz.sourbyanticheat.platform.bukkit.manager.BukkitParserDescriptorFactory;
import dev.yanianz.sourbyanticheat.platform.bukkit.manager.BukkitPermissionRegistrationManager;
import dev.yanianz.sourbyanticheat.platform.bukkit.manager.BukkitPlatformPluginManager;
import dev.yanianz.sourbyanticheat.platform.bukkit.player.BukkitPlatformPlayerFactory;
import dev.yanianz.sourbyanticheat.platform.bukkit.scheduler.bukkit.BukkitPlatformScheduler;
import dev.yanianz.sourbyanticheat.platform.bukkit.scheduler.folia.FoliaPlatformScheduler;
import dev.yanianz.sourbyanticheat.platform.bukkit.sender.BukkitSenderFactory;
import dev.yanianz.sourbyanticheat.platform.bukkit.utils.placeholder.PlaceholderAPIExpansion;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import dev.yanianz.sourbyanticheat.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.netty.channel.Channel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class SacBukkitLoaderPlugin extends JavaPlugin implements PlatformLoader {

    public static SacBukkitLoaderPlugin LOADER;

    private final LazyHolder<PlatformScheduler> scheduler = LazyHolder.simple(this::createScheduler);
    private final LazyHolder<PacketEventsAPI<?>> packetEvents = LazyHolder.simple(() -> SpigotPacketEventsBuilder.build(this));
    private final LazyHolder<BukkitSenderFactory> senderFactory = LazyHolder.simple(BukkitSenderFactory::new);
    private final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(BukkitItemResetHandler::new);
    private final LazyHolder<CommandService> commandService = LazyHolder.simple(this::createCommandService);
    private final CloudCommandAdapter commandAdapter = new BukkitParserDescriptorFactory();

    @Getter private final PlatformPlayerFactory platformPlayerFactory = new BukkitPlatformPlayerFactory();
    @Getter private final PlatformPluginManager pluginManager = new BukkitPlatformPluginManager();
    @Getter private final GrimPlugin plugin;
    @Getter private final PlatformServer platformServer = new BukkitPlatformServer();
    @Getter private final MessagePlaceHolderManager messagePlaceHolderManager = new BukkitMessagePlaceHolderManager();
    @Getter private final BukkitPermissionRegistrationManager permissionManager = new BukkitPermissionRegistrationManager();

    public SacBukkitLoaderPlugin() {
        BukkitResolverRegistrar registrar = new BukkitResolverRegistrar();
        registrar.registerAll(SacAPI.INSTANCE.getExtensionManager());
        this.plugin = registrar.resolvePlugin(this);
    }

    @Override
    public void onLoad() {
        LOADER = this;
        SacAPI.INSTANCE.load(this, this.getBukkitInitTasks());
    }

    private Initable[] getBukkitInitTasks() {
        return new Initable[] {
                new ExemptOnlinePlayersOnReload(),
                new BukkitEventManager(),
                new BukkitTickEndEvent(),
                new BukkitBStats(),
                (StartableInitable) () -> {
                    if (BukkitMessagePlaceHolderManager.hasPlaceholderAPI) {
                        new PlaceholderAPIExpansion().register();
                    }
                }
        };
    }

    @Override
    public void onEnable() {
        SacAPI.INSTANCE.start();
        wireProfileSystem();
        getServer().getMessenger().registerOutgoingPluginChannel(SacBukkitLoaderPlugin.LOADER, "sac:main");
        getServer().getPluginManager().registerEvents(new NettyInjectListener(), this);
        getServer().getPluginManager().registerEvents(new MenuRouter(), this);
        LogUtil.info("SAC ready — use /sac help for commands");
    }

    // Per-arena profiles + leniency are an enhancement layer; a failure here must
    // never block plugin enable, so the whole wiring is guarded. Check.reload/flag
    // already null-guard against an absent registry/tracker.
    private void wireProfileSystem() {
        try {
            java.nio.file.Path dataDir = getDataFolder().toPath();
            // NOTE: checks.yml is owned by the existing ConfigUpdater/SacConfigSpecs
            // pipeline (its own config-version/flavor). The profile layer lives in a
            // separate profiles.yml, so we must NOT migrate/overwrite checks.yml here.

            if (!java.nio.file.Files.exists(dataDir.resolve("profiles.yml"))) saveResource("profiles.yml", false);
            if (!java.nio.file.Files.exists(dataDir.resolve("profile-worlds.yml"))) saveResource("profile-worlds.yml", false);

            ProfileConfig profileConfig = new ProfileConfig(dataDir.resolve("profiles.yml"));
            profileConfig.reload();

            java.util.LinkedHashMap<String, Profile> worldMap = new java.util.LinkedHashMap<>();
            Profile defaultProfile = Profile.GENERIC;
            java.nio.file.Path pwPath = dataDir.resolve("profile-worlds.yml");
            if (java.nio.file.Files.exists(pwPath)) {
                Object raw = new org.yaml.snakeyaml.Yaml().load(java.nio.file.Files.newBufferedReader(pwPath));
                if (raw instanceof java.util.Map<?, ?> pwYaml) {
                    Object dp = pwYaml.get("defaultProfile");
                    if (dp instanceof String s) defaultProfile = Profile.fromString(s);
                    if (pwYaml.get("worlds") instanceof java.util.Map<?, ?> wm) {
                        for (var e : wm.entrySet()) {
                            worldMap.put(String.valueOf(e.getKey()), Profile.fromString(String.valueOf(e.getValue())));
                        }
                    }
                }
            }
            ProfileWorldMap profileWorldMap = new ProfileWorldMap(worldMap, defaultProfile);
            // Auto-map present skyblock plugins' island worlds -> SKYBLOCK profile.
            dev.yanianz.sourbyanticheat.platform.bukkit.hooks.SkyblockWorldDetector.apply(profileWorldMap);

            ProfileResolver resolver = new ProfileResolver(
                    id -> {
                        var bp = getServer().getPlayer(id);
                        return bp == null || bp.getWorld() == null ? null : bp.getWorld().getName();
                    },
                    (id, perm) -> {
                        var bp = getServer().getPlayer(id);
                        return bp != null && bp.hasPermission(perm);
                    },
                    profileWorldMap
            );
            ProfileRegistry registry = new ProfileRegistry(resolver::resolve);
            LeniencyTracker tracker = new LeniencyTracker();
            LeniencyEventBus bus = new LeniencyEventBus(registry::get, profileConfig::snapshot, tracker);

            SacAPI.INSTANCE.setProfileRegistry(registry);
            SacAPI.INSTANCE.setProfileConfig(profileConfig);
            SacAPI.INSTANCE.setLeniencyTracker(tracker);
            SacAPI.INSTANCE.setLeniencyEventBus(bus);

            var pm = getServer().getPluginManager();
            pm.registerEvents(new EnderPearlLandHandler(bus), this);
            pm.registerEvents(new FireballBoostHandler(bus), this);
            pm.registerEvents(new MLGWaterLandHandler(bus), this);
            pm.registerEvents(new KitPotionApplyHandler(bus), this);
            pm.registerEvents(new RodPullHandler(bus), this);
            pm.registerEvents(new SnowballKbHandler(bus), this);
            pm.registerEvents(new ElytraFireworkBoostHandler(bus), this);
            pm.registerEvents(new ProfileLifecycleListener(registry, tracker), this);

            // --- Soft-depend plugin hooks ---
            try {
                var cfg = dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getConfigManager().getConfig();
                java.util.function.Function<String, dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig> hookConfigFor =
                    key -> {
                        boolean enabled = cfg.getBooleanElse("hooks." + key + ".enabled", false);
                        java.util.Map<String, java.util.List<String>> abilities = new java.util.HashMap<>();
                        // mcMMO ability -> checks (keys lowercased to match HookConfig lookup)
                        for (String ability : new String[]{"berserk", "super_breaker", "giga_drill_breaker", "tree_feller"}) {
                            java.util.List<String> checks = cfg.getStringListElse("hooks." + key + "." + ability, java.util.List.of());
                            if (!checks.isEmpty()) abilities.put(ability.toLowerCase(java.util.Locale.ROOT), checks);
                        }
                        return new dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig(enabled, abilities);
                    };

                var hookManager = new dev.yanianz.sourbyanticheat.platform.bukkit.hooks.HookManager(
                    java.util.List.of(new dev.yanianz.sourbyanticheat.platform.bukkit.hooks.McMMOHook()));
                hookManager.enablePresent(this,
                    dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getExemptionRegistry(),
                    hookConfigFor);
            } catch (Throwable t) {
                dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil.warn("Failed to init plugin hooks: " + t);
            }

            LogUtil.info("Profile system ready (default=" + defaultProfile + ", " + worldMap.size() + " world mappings)");
        } catch (Throwable t) {
            LogUtil.warn("Profile system failed to initialize; anticheat runs without per-arena profiles: " + t.getMessage());
        }
    }

    private static final class ProfileLifecycleListener implements Listener {
        private final ProfileRegistry registry;
        private final LeniencyTracker tracker;

        ProfileLifecycleListener(ProfileRegistry registry, LeniencyTracker tracker) {
            this.registry = registry;
            this.tracker = tracker;
        }

        @EventHandler
        public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent e) {
            registry.invalidate(e.getPlayer().getUniqueId());
        }

        @EventHandler
        public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
            java.util.UUID id = e.getPlayer().getUniqueId();
            registry.remove(id);
            tracker.removePlayer(id);
        }
    }

    private class NettyInjectListener implements Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerJoin(PlayerJoinEvent event) {
            if (!SacNettyInjector.isInjectionFailed()) {
                Player player = event.getPlayer();
                try {
                    // Resolve the netty Channel via PacketEvents — version-robust, unlike
                    // raw NMS reflection (obfuscated field names + getField only finds
                    // public fields, which the NMS connection fields are not).
                    Object channelObj = com.github.retrooper.packetevents.PacketEvents.getAPI()
                            .getProtocolManager().getChannel(player.getUniqueId());
                    if (channelObj instanceof Channel channel) {
                        SacNettyInjector.inject(player.getUniqueId(), player.getName(), channel);
                    } else {
                        LogUtil.warn("Netty inject: no channel resolved for " + player.getName());
                    }
                } catch (Exception e) {
                    // A per-player failure must NOT disable netty globally — only a
                    // genuine netty-absent error does (handled inside the injector).
                    LogUtil.warn("Netty inject failed for " + player.getName() + ": " + e.getMessage());
                }
            }
            var sp = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer().getUniqueId());
            if (sp != null) {
                int total = sp.checkManager.allChecks.size();
                long enabled = sp.checkManager.allChecks.values().stream()
                    .filter(c -> c instanceof Check && ((Check)c).isEnabled()).count();
                LogUtil.info("Loaded " + enabled + "/" + total + " checks for " + event.getPlayer().getName());
            }
        }
    }

    @Override
    public ProxyMessenger getProxyMessenger() {
        return new ProxyMessenger() {
            @Override public void sendAlert(java.util.UUID uuid, String json) {
                ProxyForwarder.sendAlert(uuid, "backend", json);
            }
            @Override public void broadcastMessage(byte[] data) {}
            @Override public void registerAlertHandler(AlertHandler handler) {}
        };
    }

    @Override
    public dev.yanianz.sourbyanticheat.platform.api.proxy.GlobalPlayerStore getGlobalPlayerStore() {
        return new dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyNoOpMessenger();
    }

    @Override
    public dev.yanianz.sourbyanticheat.platform.api.proxy.AuditLogger getAuditLogger() {
        return new dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyNoOpMessenger();
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler.get();
    }

    @Override
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents.get();
    }

    @Override
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler.get();
    }

    @Override
    public CommandService getCommandService() {
        return commandService.get();
    }

    @Override
    public SenderFactory<CommandSender> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    @SuppressWarnings("removal")
    public void registerAPIService() {
        final SacExternalAPI externalAPI = SacAPI.INSTANCE.getExternalAPI();
        final EventBus eventBus = externalAPI.getEventBus();
        final ac.grim.grimac.api.plugin.GrimPlugin plugin = SacAPI.INSTANCE.getGrimPlugin();

        // Bridge Grim events → legacy Bukkit Event API so pre-1.3 plugins that
        // listened for ac.grim.grimac.api.events.* Bukkit events keep working.
        // Typed channel subscriptions here are plugin-bound so they go away if
        // Sac itself is disabled.

        eventBus.get(ac.grim.grimac.api.event.events.GrimJoinEvent.class).onJoin(plugin, (user) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimJoinEvent(user));
        });

        eventBus.get(ac.grim.grimac.api.event.events.GrimQuitEvent.class).onQuit(plugin, (user) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimQuitEvent(user));
        });

        eventBus.get(ac.grim.grimac.api.event.events.GrimReloadEvent.class).onReload(plugin, (success) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimReloadEvent(success));
        });

        eventBus.get(ac.grim.grimac.api.event.events.FlagEvent.class).onFlag(plugin, (user, check, verbose, cancelled) -> {
            ac.grim.grimac.api.events.FlagEvent bukkitEvent =
                    new ac.grim.grimac.api.events.FlagEvent(user, check, verbose);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        eventBus.get(ac.grim.grimac.api.event.events.CommandExecuteEvent.class).onCommandExecute(plugin, (user, check, verbose, command, cancelled) -> {
            ac.grim.grimac.api.events.CommandExecuteEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CommandExecuteEvent(user, check, verbose, command);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        eventBus.get(ac.grim.grimac.api.event.events.CompletePredictionEvent.class).onCompletePrediction(plugin, (user, check, offset, cancelled) -> {
            // Legacy Bukkit event has a verbose field that the new channel event does not; pass empty.
            ac.grim.grimac.api.events.CompletePredictionEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CompletePredictionEvent(user, check, "", offset);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        GrimAPIProvider.init(externalAPI);
        Bukkit.getServicesManager().register(GrimAbstractAPI.class, externalAPI, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(SacAbstractAPI.class, externalAPI, this, ServicePriority.Normal);
    }

    private PlatformScheduler createScheduler() {
        return SacAPI.INSTANCE.getPlatform() == Platform.FOLIA ? new FoliaPlatformScheduler() : new BukkitPlatformScheduler();
    }

    private CommandService createCommandService() {
        try {
            return new CloudCommandService(this::createCloudCommandManager, commandAdapter);
        } catch (Throwable t) {
            LogUtil.warn("CRITICAL: Failed to initialize Command Framework. " +
                    "SAC will continue to run with no commands.", t);
            return () -> {};
        }
    }

    private CommandManager<Sender> createCloudCommandManager() {
        LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        );
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            try {
                manager.registerBrigadier();
                CloudBrigadierManager<Sender, ?> cbm = manager.brigadierManager();
                cbm.settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);
            } catch (Throwable t) {
                LogUtil.error("Failed to register Brigadier native completions. Falling back to standard completions.", t);
            }
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }
        return manager;
    }

    public BukkitSenderFactory getBukkitSenderFactory() {
        return senderFactory.get();
    }
}
