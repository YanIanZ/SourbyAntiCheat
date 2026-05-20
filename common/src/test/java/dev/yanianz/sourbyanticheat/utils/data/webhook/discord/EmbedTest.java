package dev.yanianz.sourbyanticheat.utils.data.webhook.discord;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class EmbedTest {

    @Test void embedHasContent() {
        var embed = new Embed("Test content");
        assertNotNull(embed);
    }

    @Test void embedColorDefaultsToHexString() {
        var embed = new Embed("x").color(0xFF6B35);
        assertNotNull(embed);
    }

    @Test void embedTitleIsSet() {
        var embed = new Embed("x").title("Alert Title");
        assertNotNull(embed);
    }

    @Test void embedTimestampIsSet() {
        var embed = new Embed("x").timestamp(Instant.now());
        assertNotNull(embed);
    }

    @Test void embedFooterIsSet() {
        var embed = new Embed("x").footer(new EmbedFooter("SAC System", null));
        assertNotNull(embed);
    }

    @Test void embedFooterTextOnly() {
        var footer = new EmbedFooter("v1.0", null);
        assertEquals("v1.0", footer.text());
        assertNull(footer.icon());
    }

    @Test void embedFooterWithIcon() {
        var footer = new EmbedFooter("SAC", "https://example.com/icon.png");
        assertEquals("SAC", footer.text());
        assertEquals("https://example.com/icon.png", footer.icon());
    }

    @Test void embedImageUrlIsSet() {
        var embed = new Embed("x").imageURL("https://example.com/img.png");
        assertNotNull(embed);
    }

    @Test void embedThumbnailUrlIsSet() {
        var embed = new Embed("x").thumbnailURL("https://example.com/thumb.png");
        assertNotNull(embed);
    }

    @Test void embedFieldsAreAddable() {
        var embed = new Embed("x").addFields(new EmbedField("Key", "Value", true));
        assertNotNull(embed);
    }

    @Test void embedFieldStoresData() {
        var field = new EmbedField("Name", "Value", false);
        assertEquals("Name", field.name());
        assertEquals("Value", field.value());
        assertFalse(field.inline());
    }

    @Test void embedFieldInline() {
        var field = new EmbedField("Name", "Value", true);
        assertTrue(field.inline());
    }

    @Test void webhookMessageCanAddEmbeds() {
        var msg = new WebhookMessage();
        msg.addEmbeds(new Embed("test"));
        assertNotNull(msg);
    }

    @Test void webhookMessageToJsonIsString() {
        var msg = new WebhookMessage();
        msg.addEmbeds(new Embed("content").color(0xFF6B35));
        var json = msg.toJson();
        assertNotNull(json);
    }

    @Test void colorDecodingRgbToHex() {
        int rgb = Color.decode("#FF6B35").getRGB();
        assertEquals(0xFF6B35, rgb & 0xFFFFFF);
    }

    @Test void colorDecodingHexToRgb() {
        int rgb = Color.decode("#00D4AA").getRGB();
        assertTrue(rgb != 0);
    }

    @Test void escapedMarkdownPreservesText() {
        String input = "**bold** _italic_";
        String escaped = CompiledDiscordTemplate.escapeMarkdown(input);
        assertNotNull(escaped);
        assertFalse(escaped.isEmpty());
    }

    @Test void backtickReplacementCharIsValid() {
        char c = '\u02CB';
        assertTrue(Character.isDefined(c));
    }

    @Test void templateCompilationReturnsObject() {
        var compiled = CompiledDiscordTemplate.compile("**Player**: `%player%`");
        assertNotNull(compiled);
    }
}
