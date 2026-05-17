package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "ForceField", configName = "forcefield", decay = 0.02, setback = 10, stableKey = "cross.forcefield")
public class ForceField extends Check implements PacketCheck {

    private int attacksThisTick;
    private int distinctTargetsThisTick;
    private int lastEntity;
    private int sustainedTicks;
    private int buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int maxAttacksPerTick = 1;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public ForceField(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxAttacksPerTick = config.getIntElse(base + "max-attacks-per-tick", 1);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Legitimate fast multi-mob hits: attacking several distinct entities in a
            // single tick once is normal (e.g. swing landing on a mob crowd). Only treat
            // it as suspicious when the burst is sustained across consecutive ticks.
            if (attacksThisTick > maxAttacksPerTick) {
                sustainedTicks++;
                if (sustainedTicks >= 2) {
                    boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
                    SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
                    boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
                    buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
                    if (buffer > 3) {
                        flagAndAlert(String.format("attacks=%d targets=%d sustained=%d netty=%.1f/s spartan=%s",
                            attacksThisTick, distinctTargetsThisTick, sustainedTicks,
                            player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                    }
                }
            } else {
                sustainedTicks = 0;
                buffer = Math.max(0, buffer - 1);
                reward();
            }
            attacksThisTick = 0;
            distinctTargetsThisTick = 0;
            lastEntity = -1;
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int eid = interact.getEntityId();
        attacksThisTick++;
        if (eid != lastEntity) {
            distinctTargetsThisTick++;
            lastEntity = eid;
        }
    }
}
