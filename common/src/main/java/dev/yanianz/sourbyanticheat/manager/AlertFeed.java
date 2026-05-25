package dev.yanianz.sourbyanticheat.manager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Bounded, newest-first ring buffer of recent staff alerts. One global
 * instance ({@link #GLOBAL}) is populated by the alert pipeline and read by
 * the GUI alerts panel (a later plan). Thread-safe; pure (no SacAPI dependency).
 */
public final class AlertFeed {

    /** Immutable snapshot of a single staff alert. */
    public record Entry(String player, UUID uuid, String check, int vl, String verbose, long ts) {}

    public static final AlertFeed GLOBAL = new AlertFeed(50);

    private final int capacity;
    private final Deque<Entry> entries = new ArrayDeque<>();

    public AlertFeed(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public synchronized void push(Entry entry) {
        entries.addFirst(entry);
        while (entries.size() > capacity) entries.removeLast();
    }

    /** @return snapshot of recent entries, newest first. */
    public synchronized List<Entry> recent() {
        return new ArrayList<>(entries);
    }
}
