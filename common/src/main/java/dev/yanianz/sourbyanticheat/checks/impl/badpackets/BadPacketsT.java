package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsT", stableKey = "sac.badpackets.invalid_interact_vector")
public class BadPacketsT extends Check implements PacketCheck {

    // Protocol interaction-vector bounds: the hit point of an INTERACT_AT packet must
    // fall within the target player's bounding box (slightly inflated for float noise).
    private static final double BASE_MAX_HORIZONTAL = 0.3001;
    private static final double BASE_MIN_VERTICAL = -0.0001;
    private static final double BASE_MAX_VERTICAL = 1.8001;
    // 1.7/1.8 clients use a different hitbox expansion than 1.9+.
    private static final double LEGACY_EXPANSION = 0.1;

    private final double maxHorizontalDisplacement;
    private final double minVerticalDisplacement;
    private final double maxVerticalDisplacement;

    public BadPacketsT(final SacPlayer player) {
        super(player);
        // 1.7 and 1.8 seem to have different hitbox "expansion" values than 1.9+
        // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872458702
        // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872533497
        double expansion = player.getClientVersion().isOlderThan(ClientVersion.V_1_9) ? LEGACY_EXPANSION : 0;
        maxHorizontalDisplacement = BASE_MAX_HORIZONTAL + expansion;
        minVerticalDisplacement = BASE_MIN_VERTICAL - expansion;
        maxVerticalDisplacement = BASE_MAX_VERTICAL + expansion;
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY)) {
            final WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            // Only INTERACT_AT actually has an interaction vector
            if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) return;
            Vector3d targetVector = wrapper.getLocation();
            if (targetVector == null) return; // shouldn't ever happen, but whatever

            final PacketEntity packetEntity = player.compensatedEntities.getEntity(wrapper.getEntityId());
            // Don't continue if the compensated entity hasn't been resolved
            if (packetEntity == null) {
                return;
            }

            // Make sure our target entity is actually a player (Player NPCs work too)
            if (!EntityTypes.PLAYER.equals(packetEntity.type)) {
                // We can't check for any entity that is not a player
                return;
            }

            // Perform the interaction vector check
            // TODO:
            //  27/12/2023 - Dynamic values for more than just one entity type?
            //  28/12/2023 - Player-only is fine
            //  30/12/2023 - Expansions differ in 1.9+
            final float scale = (float) packetEntity.getAttributeValue(Attributes.SCALE);
            if (targetVector.y > (minVerticalDisplacement * scale) && targetVector.y < (maxVerticalDisplacement * scale)
                    && Math.abs(targetVector.x) < (maxHorizontalDisplacement * scale)
                    && Math.abs(targetVector.z) < (maxHorizontalDisplacement * scale)) {
                reward();
                return;
            }

            // NOTE: a sit/sneak pose on the TARGET shrinks its box; the bounds above are the
            // full-standing box, so a shrunk target only makes the check more lenient (no
            // false positive) — a tighter pose-aware bound is a future detection improvement.

            // Log the vector
            final String verbose = String.format("%.5f/%.5f/%.5f",
                    targetVector.x, targetVector.y, targetVector.z);
            // We could pretty much ban the player at this point
            flagAndAlert(verbose);
        }
    }
}
