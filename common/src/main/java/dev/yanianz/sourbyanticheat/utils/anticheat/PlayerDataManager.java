package dev.yanianz.sourbyanticheat.utils.anticheat;

import dev.yanianz.sourbyanticheat.SacAPI;
import ac.grim.grimac.api.event.events.GrimJoinEvent;
import ac.grim.grimac.api.event.events.GrimQuitEvent;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    // Holder — PlayerDataManager is constructed inside SacAPI's ctor, so a
    // plain static-final would see a null SacAPI.INSTANCE. Holder init runs
    // on first fire, after SacAPI is fully built.
    private static final class Channels {
        static final GrimJoinEvent.Channel JOIN = SacAPI.INSTANCE.getEventBus().get(GrimJoinEvent.class);
        static final GrimQuitEvent.Channel QUIT = SacAPI.INSTANCE.getEventBus().get(GrimQuitEvent.class);
    }

    public final Collection<User> exemptUsers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<User, SacPlayer> playerDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, SacPlayer> playerByUuid = new ConcurrentHashMap<>();

    @Nullable
    public SacPlayer getPlayer(final @NotNull UUID uuid) {
        SacPlayer cached = playerByUuid.get(uuid);
        if (cached != null) {
            if (cached.platformPlayer != null && cached.platformPlayer.isExternalPlayer()) return null;
            return cached;
        }
        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(uuid);
        User user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        return getPlayer(user);
    }

    @Nullable
    public SacPlayer getPlayer(final @NotNull User user) {
        @Nullable SacPlayer player = playerDataMap.get(user);
        if (player != null && player.platformPlayer != null && player.platformPlayer.isExternalPlayer())
            return null;
        return player;
    }

    public boolean shouldCheck(@NotNull User user) {
        if (exemptUsers.contains(user)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;

        if (user.getUUID() != null) {
            // Bedrock players don't have Java movement
            if (GeyserUtil.isBedrockPlayer(user.getUUID())) {
                exemptUsers.add(user);
                return false;
            }

            // Has exempt permission
            SacPlayer grimPlayer = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(user);
            if (grimPlayer != null && grimPlayer.hasPermission("sac.exempt")) {
                exemptUsers.add(user);
                return false;
            }

            // Geyser formatted player string
            // This will never happen for Java players, as the first character in the 3rd group is always 4 (xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxxx)
            if (user.getUUID().toString().startsWith("00000000-0000-0000-0009")) {
                exemptUsers.add(user);
                return false;
            }
        }

        return true;
    }

    public void addUser(final @NotNull User user) {
        if (shouldCheck(user)) {
            SacPlayer player = new SacPlayer(user);
            playerDataMap.put(user, player);
            if (user.getUUID() != null) {
                playerByUuid.put(user.getUUID(), player);
            }
            Channels.JOIN.fire(player);
        }
    }

    public SacPlayer remove(final @NotNull User user) {
        SacPlayer removed = playerDataMap.remove(user);
        if (removed != null && user.getUUID() != null) {
            playerByUuid.remove(user.getUUID(), removed);
        }
        return removed;
    }

    public void onDisconnect(User user) {
        SacPlayer grimPlayer = remove(user);
        if (grimPlayer != null) Channels.QUIT.fire(grimPlayer);
        exemptUsers.remove(user);

        UUID uuid = user.getProfile().getUUID();

        // All cleanup paths should call onDisconnect; routing the session-close + toggle
        // eviction here means a stuck PE event (or a JVM-level channel
        // close that doesn't surface as UserDisconnectEvent) doesn't leak an open session.
        // hooks/toggles are NOOP when the datastore is disabled or its init failed
        // AND go NOOP mid-session if an operator runs /grim reload after flipping database.enabled to false
        // a player who joined under the prior (enabled) config and disconnects post-reload has no live writer to fire onQuit, so their session stays open (row closed_at IS NULL).
        // The next datastore-enabled boot's crash sweep stamps closed_at = last_activity for still-open rows; permanently-disabled-after-the-fact leaves the row untouched until DB is enabled again.
        SacAPI.INSTANCE.getDataStoreLifecycle().liveWriteHooks()
                .onQuitFromUserDisconnect(user, grimPlayer, System.currentTimeMillis());
        if (uuid != null) {
            SacAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore().evict(uuid);
        }

        // Check if calling async is safe
        if (uuid == null)
            return; // folia doesn't like null getPlayer()

        SacAPI.INSTANCE.getAlertManager().handlePlayerQuit(
                SacAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(uuid)
        );
        SacAPI.INSTANCE.getExemptionRegistry().clear(uuid);

        SacAPI.INSTANCE.getSpectateManager().onQuit(uuid);

        // TODO (Cross-platform) confirm this is 100% correct and will always remove players from cache when necessary
        SacAPI.INSTANCE.getPlatformPlayerFactory().invalidatePlayer(uuid);
    }

    public Collection<SacPlayer> getEntries() {
        return playerDataMap.values();
    }

    public int size() {
        return playerDataMap.size();
    }
}
