package dev.yanianz.sourbyanticheat.profile;

import ac.grim.grimac.api.config.ConfigManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ProfileAwareConfigView implements ConfigManager {
    private final ConfigManager base;
    private final ProfileConfigSnapshot.ProfileSection section;

    public ProfileAwareConfigView(ConfigManager base, ProfileConfigSnapshot.ProfileSection section) {
        this.base = base;
        this.section = section;
    }

    private Object overrideFor(String dottedKey) {
        int dot = dottedKey.indexOf('.');
        if (dot < 0) return null;
        String checkName = dottedKey.substring(0, dot);
        String prop = dottedKey.substring(dot + 1);
        return section.override(checkName, prop);
    }

    @Override
    public double getDoubleElse(String key, double def) {
        Object o = overrideFor(key);
        if (o instanceof Number n) return n.doubleValue();
        return base.getDoubleElse(key, def);
    }

    @Override
    public int getIntElse(String key, int def) {
        Object o = overrideFor(key);
        if (o instanceof Number n) return n.intValue();
        return base.getIntElse(key, def);
    }

    @Override
    public long getLongElse(String key, long def) {
        Object o = overrideFor(key);
        if (o instanceof Number n) return n.longValue();
        return base.getLongElse(key, def);
    }

    @Override
    public boolean getBooleanElse(String key, boolean def) {
        // "checks.enabled.<CheckName>" — short-circuit when profile disables the check
        if (key.startsWith("checks.enabled.")) {
            String name = key.substring("checks.enabled.".length());
            if (section.isDisabled(name)) return false;
        }
        Object o = overrideFor(key);
        if (o instanceof Boolean b) return b;
        return base.getBooleanElse(key, def);
    }

    @Override
    public String getStringElse(String key, String def) {
        Object o = overrideFor(key);
        if (o instanceof String s) return s;
        return base.getStringElse(key, def);
    }

    // delegate remaining ConfigManager methods to base
    @Override public String getString(String key) { return base.getString(key); }
    @Override public List<String> getStringList(String key) { return base.getStringList(key); }
    @Override public List<String> getStringListElse(String key, List<String> def) { return base.getStringListElse(key, def); }
    @Override public <T> T get(String key) { return base.get(key); }
    @Override public <T> T getElse(String key, T def) { return base.getElse(key, def); }
    @Override public <K, V> Map<K, V> getMap(String key) { return base.getMap(key); }
    @Override public <K, V> Map<K, V> getMapElse(String key, Map<K, V> def) { return base.getMapElse(key, def); }
    @Override public <T> List<T> getList(String key) { return base.getList(key); }
    @Override public <T> List<T> getListElse(String key, List<T> def) { return base.getListElse(key, def); }
    @Override public boolean hasLoaded() { return base.hasLoaded(); }

    @Override public void reload() { base.reload(); }
    @Override public boolean isLoadedAsync() { return base.isLoadedAsync(); }
    @Override public CompletableFuture<Boolean> reloadAsync() { return base.reloadAsync(); }
}
