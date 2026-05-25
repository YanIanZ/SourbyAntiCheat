package dev.yanianz.sourbyanticheat.utils.anticheat;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;

import static org.junit.jupiter.api.Assertions.*;

class NetErrorsTest {

    @Test
    void unresolvedAddressIsOffline() {
        assertTrue(NetErrors.isOffline(new UnresolvedAddressException()));
    }

    @Test
    void connectExceptionIsOffline() {
        assertTrue(NetErrors.isOffline(new ConnectException("refused")));
    }

    @Test
    void unknownHostIsOffline() {
        assertTrue(NetErrors.isOffline(new UnknownHostException("nope")));
    }

    @Test
    void offlineDetectedThroughCauseChain() {
        Throwable wrapped = new RuntimeException("outer",
                new ConnectException(""){{ initCause(new UnresolvedAddressException()); }});
        assertTrue(NetErrors.isOffline(wrapped));
    }

    @Test
    void genericExceptionIsNotOffline() {
        assertFalse(NetErrors.isOffline(new IllegalStateException("bug")));
    }

    @Test
    void nullIsNotOffline() {
        assertFalse(NetErrors.isOffline(null));
    }
}
