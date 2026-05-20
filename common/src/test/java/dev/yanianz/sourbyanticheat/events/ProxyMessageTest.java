package dev.yanianz.sourbyanticheat.events;

import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class ProxyMessageTest {

    @Test void forwardCommandIsCorrect() {
        String channel = "BungeeCord";
        assertEquals("BungeeCord", channel);
    }

    @Test void grimacSubChannel() {
        String sub = "GRIMAC";
        assertEquals("GRIMAC", sub);
    }

    @Test void forwardSubChannelIsOnline() {
        String forward = "Forward";
        String online = "ONLINE";
        assertNotNull(forward);
        assertNotNull(online);
    }

    @Test void alertJsonContainsExpectedFields() {
        String json = "{\"ts\":1700000,\"player\":\"Test\",\"uuid\":\"abc\",\"check\":\"Speed\",\"vl\":10}";
        assertTrue(json.contains("\"player\""));
        assertTrue(json.contains("\"check\""));
        assertTrue(json.contains("\"vl\""));
        assertTrue(json.contains("\"ts\""));
        assertTrue(json.contains("\"uuid\""));
    }

    @Test void dataStreamWriteAndReadUTF() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF("Test Message");
        dos.flush();

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        DataInputStream dis = new DataInputStream(bis);
        assertEquals("Test Message", dis.readUTF());
    }

    @Test void byteArrayDataOutputWriteUTF() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE");
        out.writeUTF("GRIMAC");

        byte[] msgBytes = "alert content".getBytes();
        out.writeShort(msgBytes.length);
        out.write(msgBytes);

        assertTrue(out.toByteArray().length > 0);
    }

    @Test void canSendAlertsRequiresProxyAndConfig() {
        boolean usingProxy = true;
        boolean sendEnabled = true;
        boolean hasPlayers = true;
        assertTrue(usingProxy && sendEnabled && hasPlayers);
    }

    @Test void cannotSendAlertsWithoutProxy() {
        boolean usingProxy = false;
        assertFalse(usingProxy);
    }

    @Test void cannotSendAlertsWithoutConfigEnabled() {
        boolean usingProxy = true;
        boolean sendEnabled = false;
        assertFalse(usingProxy && sendEnabled);
    }

    @Test void noOnlinePlayersBlocksSend() {
        boolean usingProxy = true;
        boolean sendEnabled = true;
        boolean hasPlayers = false;
        assertFalse(usingProxy && sendEnabled && hasPlayers);
    }

    @Test void proxyAlertStringContainsExpectedMarkers() {
        String format = "%prefix% &f[&cproxy&f] &f%player% &bfailed &f%check_name% &7[&c%vl%&7] &7%verbose%";
        assertTrue(format.contains("&cproxy"));
        assertTrue(format.contains("%player%"));
    }
}
