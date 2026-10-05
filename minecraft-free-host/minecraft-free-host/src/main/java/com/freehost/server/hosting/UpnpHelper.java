package com.freehost.server.hosting;

import com.freehost.common.NetLog;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Ouvre automatiquement le port sur la box via UPnP et détermine l'IP publique. */
public final class UpnpHelper {
    public record Result(boolean mapped, String externalIp, String warning) {}

    private GatewayDevice device;
    private int mappedPort = -1;

    public Result open(int port) {
        String ext = null;
        boolean mapped = false;
        String warn = null;
        try {
            GatewayDiscover discover = new GatewayDiscover();
            discover.setTimeout(3000);
            discover.discover();
            GatewayDevice g = discover.getValidGateway();
            if (g != null) {
                ext = g.getExternalIPAddress();
                mapped = g.addPortMapping(port, port, g.getLocalAddress().getHostAddress(), "TCP", "FreeHost");
                if (mapped) {
                    device = g;
                    mappedPort = port;
                    NetLog.add("UPnP: port " + port + " opened on router");
                } else {
                    warn = "Router refused UPnP mapping: open TCP port " + port + " manually";
                }
            } else {
                warn = "No UPnP router found: open TCP port " + port + " manually";
            }
        } catch (Exception e) {
            warn = "UPnP failed (" + e.getClass().getSimpleName() + "): open TCP port " + port + " manually";
        }
        if (ext == null || ext.isBlank() || ext.equals("0.0.0.0")) ext = fetchPublicIp();
        if (ext != null && isPrivate(ext)) {
            warn = "Your ISP shares your IP (CGNAT): friends outside your network cannot join";
        }
        if (warn != null) NetLog.add(warn);
        return new Result(mapped, ext, warn);
    }

    public synchronized void close() {
        if (device != null && mappedPort > 0) {
            try {
                device.deletePortMapping(mappedPort, "TCP");
                NetLog.add("UPnP: port " + mappedPort + " closed");
            } catch (Exception ignored) {
            }
        }
        device = null;
        mappedPort = -1;
    }

    private static String fetchPublicIp() {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.ipify.org"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body().trim();
            return body.matches("\\d{1,3}(\\.\\d{1,3}){3}") ? body : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isPrivate(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            byte[] b = a.getAddress();
            boolean cgnat = b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64;
            return a.isSiteLocalAddress() || a.isLoopbackAddress() || a.isAnyLocalAddress() || cgnat;
        } catch (Exception e) {
            return false;
        }
    }
}
