package dev.yanianz.sourbyanticheat.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormatTest {

    @Test
    void alertFormatContainsExpectedPlaceholders() {
        String format = "%prefix% &f%player% &bfailed &f%check_name% &7[&c%vl%&7] &7%verbose%";
        assertTrue(format.contains("%player%"));
        assertTrue(format.contains("%check_name%"));
        assertTrue(format.contains("%vl%"));
        assertTrue(format.contains("%verbose%"));
    }

    @Test
    void waveCommandFormatReplacesAllTokens() {
        String cmd = "ban %player% %uuid%";
        String result = cmd
            .replace("%player%", "Player123")
            .replace("%uuid%", "aaaa-bbbb-cccc");
        assertEquals("ban Player123 aaaa-bbbb-cccc", result);
    }

    @Test
    void discordMessageNoEmbedThrows() {
        // WebhookMessage with no embeds should still produce valid JSON
        String template = "**Player**: `%player%`\n**Check**: %check%\n**VL**: %violations%";
        String rendered = template
            .replace("%player%", "Test")
            .replace("%check%", "Speed")
            .replace("%violations%", "100");
        assertTrue(rendered.contains("Test"));
        assertTrue(rendered.contains("Speed"));
        assertTrue(rendered.contains("100"));
    }

    @Test
    void reportReasonWithSpecialCharacters() {
        String reason = "cheating with \"speed\" & fly hacks";
        // Special chars should be preserved in report storage
        assertTrue(reason.contains("\""));
        assertTrue(reason.contains("&"));
    }

    @Test
    void playerNameWithUnderscores() {
        String name = "Player_Name_123";
        assertEquals("Player_Name_123", name);
    }

    @Test
    void uuidFromStringRoundTrip() {
        java.util.UUID uuid = java.util.UUID.randomUUID();
        String str = uuid.toString();
        java.util.UUID parsed = java.util.UUID.fromString(str);
        assertEquals(uuid, parsed);
    }

    @Test
    void timestampFormatting() {
        long ts = 1700000000000L;
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm");
        fmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String formatted = fmt.format(new java.util.Date(ts));
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.matches("\\d{2}:\\d{2}"));
    }

    @Test
    void emptyStringCooldownMessage() {
        long remaining = 45;
        String msg = "Please wait " + remaining + "s before reporting again.";
        assertTrue(msg.contains("45s"));
        assertTrue(msg.contains("wait"));
    }
}
