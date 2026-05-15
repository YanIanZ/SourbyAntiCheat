package dev.yanianz.sourbyanticheat.api;

import ac.grim.grimac.api.GrimAbstractAPI;

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
}
