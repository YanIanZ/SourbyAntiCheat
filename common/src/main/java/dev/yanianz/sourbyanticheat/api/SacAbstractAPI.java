package dev.yanianz.sourbyanticheat.api;

import ac.grim.grimac.api.GrimAbstractAPI;
import dev.yanianz.sourbyanticheat.profile.Profile;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;

import java.util.UUID;

/**
 * SAC-branded API interface for external plugin consumption.
 * Extends GrimAbstractAPI for backward compatibility with GrimAPI consumers.
 *
 * <p>Usage via Bukkit Services API:</p>
 * <pre>{@code
 * SacAbstractAPI api = Bukkit.getServicesManager().load(SacAbstractAPI.class);
 * if (api != null) {
 *     // use SAC API
 * }
 * }</pre>
 *
 * @author YanIanZ
 */
public interface SacAbstractAPI extends GrimAbstractAPI {

    Profile getProfile(UUID playerId);
    void setProfile(UUID playerId, Profile profile);
    void clearProfile(UUID playerId);

    void grantLeniency(UUID playerId, String checkName, long durationMillis);
    void grantLeniencyAll(UUID playerId, long durationMillis);
    void revokeLeniency(UUID playerId, String checkName);
    boolean hasLeniency(UUID playerId, String checkName);

    void fireLeniency(LeniencyId event, UUID playerId, int amplifier);
}
