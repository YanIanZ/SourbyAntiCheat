package dev.yanianz.sourbyanticheat.utils.anticheat;

import lombok.experimental.UtilityClass;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.nio.channels.UnresolvedAddressException;

/**
 * Classifies whether a throwable represents an "offline / host unreachable"
 * network failure (expected on a LAN/dev box with no internet) versus a real
 * bug. Pure and dependency-free so it is unit-testable.
 */
@UtilityClass
public class NetErrors {

    /** @return true if {@code t} or any cause is a host-unreachable network error. */
    public boolean isOffline(Throwable t) {
        for (Throwable c = t; c != null && c != c.getCause(); c = c.getCause()) {
            if (c instanceof UnresolvedAddressException
                    || c instanceof ConnectException
                    || c instanceof UnknownHostException
                    || c instanceof HttpConnectTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
