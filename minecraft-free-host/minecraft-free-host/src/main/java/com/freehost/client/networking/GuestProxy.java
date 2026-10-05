package com.freehost.client.networking;

import com.freehost.common.NetLog;
import com.freehost.common.protocol.Invite;
import com.freehost.common.protocol.Wire;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Faux serveur Minecraft local (127.0.0.1:port aléatoire). Minecraft s'y connecte comme à un serveur normal ;
 * le proxy fait la poignée de main avec l'hôte (code secret) puis relaie tout le trafic.
 */
public final class GuestProxy {
    private static ServerSocket server;

    private GuestProxy() {}

    public static synchronized int start(Invite inv) throws IOException {
        stop();
        ServerSocket ss = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
        server = ss;
        Thread.ofVirtual().name("freehost-guest-proxy").start(() -> loop(ss, inv));
        return ss.getLocalPort();
    }

    public static synchronized void stop() {
        Wire.closeQuiet(server);
        server = null;
    }

    private static void loop(ServerSocket ss, Invite inv) {
        while (!ss.isClosed()) {
            try {
                Socket local = ss.accept();
                Thread.ofVirtual().start(() -> forward(local, inv));
            } catch (IOException e) {
                break;
            }
        }
    }

    private static void forward(Socket local, Invite inv) {
        Socket remote = new Socket();
        try {
            remote.connect(new InetSocketAddress(InetAddress.getByAddress(inv.ip()), inv.port()), 5000);
            remote.setSoTimeout(5000);
            remote.setTcpNoDelay(true);
            local.setTcpNoDelay(true);
            Wire.writeLine(remote.getOutputStream(), Wire.HELLO + " " + Wire.hex(inv.secret()) + " JOIN");
            String reply = Wire.readLine(remote.getInputStream(), 64);
            if (!"OK".equals(reply)) {
                NetLog.add("Host refused connection: " + reply);
                Wire.closeQuiet(remote);
                Wire.closeQuiet(local);
                return;
            }
            remote.setSoTimeout(0);
            Wire.bridge(local, remote, null);
        } catch (IOException e) {
            NetLog.add("Proxy error: " + e.getMessage());
            Wire.closeQuiet(remote);
            Wire.closeQuiet(local);
        }
    }
}
