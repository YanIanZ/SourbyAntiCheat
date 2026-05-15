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
import dev.yanianz.sourbyanticheat.netty.SacNettyInjector;
import dev.yanianz.sourbyanticheat.platform.bukkit.gui.SacGUI;
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
        getServer().getPluginManager().registerEvents(new NettyInjectListener(), this);
        getServer().getPluginManager().registerEvents(new SacGUI(), this);
    }

    private class NettyInjectListener implements Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerJoin(PlayerJoinEvent event) {
            if (SacNettyInjector.isInjectionFailed()) return;
            Player player = event.getPlayer();
            try {
                Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = craftPlayer.getClass().getField("connection").get(craftPlayer);
                Object networkManager = connection.getClass().getField("connection").get(connection);
                Channel channel = (Channel) networkManager.getClass().getField("channel").get(networkManager);
                SacNettyInjector.inject(player.getName(), channel);
            } catch (Exception e) {
                LogUtil.warn("Failed to inject Netty handler for " + player.getName() + " — falling back to PacketEvents-only");
                SacNettyInjector.markInjectionFailed();
            }
        }
    }

    @Override
    public void onDisable() {
        SacAPI.INSTANCE.stop();
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
