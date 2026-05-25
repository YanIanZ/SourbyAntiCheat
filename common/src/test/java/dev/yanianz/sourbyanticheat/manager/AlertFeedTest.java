package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertFeedTest {

    private AlertFeed.Entry entry(String name, int vl, long ts) {
        return new AlertFeed.Entry(name, UUID.randomUUID(), "Reach", vl, "v", ts);
    }

    @Test
    void recentIsNewestFirst() {
        AlertFeed feed = new AlertFeed(10);
        feed.push(entry("a", 1, 100));
        feed.push(entry("b", 2, 200));
        List<AlertFeed.Entry> recent = feed.recent();
        assertEquals("b", recent.get(0).player());
        assertEquals("a", recent.get(1).player());
    }

    @Test
    void capacityBoundDropsOldest() {
        AlertFeed feed = new AlertFeed(2);
        feed.push(entry("a", 1, 100));
        feed.push(entry("b", 2, 200));
        feed.push(entry("c", 3, 300));
        List<AlertFeed.Entry> recent = feed.recent();
        assertEquals(2, recent.size());
        assertEquals("c", recent.get(0).player());
        assertEquals("b", recent.get(1).player());
    }

    @Test
    void emptyFeedReturnsEmptyList() {
        assertTrue(new AlertFeed(5).recent().isEmpty());
    }
}
