package com.freehost.server.hosting;

import com.freehost.common.NetLog;
import com.freehost.common.protocol.Wire;
import com.freehost.common.security.RateLimiter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Porte d'entrée publique de l'hôte. Elle vérifie le secret du code d'invitation,
 * l'expiration, le nombre de joueurs et les tentatives, puis relie la connexion
 * au serveur Minecraft local (127.0.0.1). Le monde ne quitte jamais le PC.
 */
public final class Gateway {
    private static final int MAX_CONNECTIONS = 32;

    private final int port;
    private final int mcPort;
    private volatile byte[] secret;
    private volatile long expiresAtMs;
    private final RateLimiter limiter = new RateLimiter(5, 60_000, 300_000);
    private final AtomicInteger active = new AtomicInteger();
    private volatile ServerSocket server;
    private volatile boolean running;

    public Gateway(int port, int mcPort, byte[] secret, long expiresAtMs) {
        this.port = port;
        this.mcPort = mcPort;
        this.secret = secret;
        this.expiresAtMs = expiresAtMs;
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(port));
        server = ss;
        running = true;
        Thread.ofVirtual().name("freehost-gateway").start(this::acceptLoop);
        NetLog.add("Gateway listening on TCP " + port);
    }

    public void update(byte[] newSecret, long newExpiresAtMs) {
        this.secret = newSecret;
        this.expiresAtMs = newExpiresAtMs;
    }

    public void stop() {
        running = false;
        Wire.closeQuiet(server);
        NetLog.add("Gateway stopped");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = server.accept();
                Thread.ofVirtual().start(() -> handle(s));
            } catch (IOException e) {
                if (running) NetLog.add("Accept error: " + e.getMessage());
            }
        }
    }

    private void handle(Socket s) {
        String ip = s.getInetAddress().getHostAddress();
        boolean bridged = false;
        active.incrementAndGet();
        try {
            if (limiter.isBlocked(ip) || active.get() > MAX_CONNECTIONS) {
                return;
            }
            s.setSoTimeout(5000);
            s.setTcpNoDelay(true);

            String line;
            try {
                line = Wire.readLine(s.getInputStream(), 80);
            } catch (IOException e) {
                limiter.fail(ip);
                return;
            }
            String[] p = line.split(" ");
            if (p.length != 3 || !Wire.HELLO.equals(p[0])) {
                limiter.fail(ip);
                NetLog.add("Rejected malformed handshake from " + ip);
                return;
            }
            byte[] given = Wire.unhex(p[1]);
            if (given == null || !MessageDigest.isEqual(given, secret)) {
                limiter.fail(ip);
                NetLog.add("Rejected invalid code from " + ip);
                Wire.writeLine(s.getOutputStream(), "ERR DENIED");
                return;
            }
            if (expiresAtMs > 0 && System.currentTimeMillis() > expiresAtMs) {
                Wire.writeLine(s.getOutputStream(), "ERR EXPIRED");
                return;
            }
            if (HostManager.isFull()) {
                Wire.writeLine(s.getOutputStream(), "ERR FULL");
                return;
            }
            if ("CHECK".equals(p[2])) {
                Wire.writeLine(s.getOutputStream(), "OK");
                return;
            }
            if (!"JOIN".equals(p[2])) {
                limiter.fail(ip);
                return;
            }
            Socket local = new Socket("127.0.0.1", mcPort);
            local.setTcpNoDelay(true);
            Wire.writeLine(s.getOutputStream(), "OK");
            s.setSoTimeout(0);
            NetLog.add("Guest connected from " + ip);
            bridged = true;
            Wire.bridge(s, local, () -> {
                active.decrementAndGet();
                NetLog.add("Guest disconnected (" + ip + ")");
            });
        } catch (IOException e) {
            NetLog.add("Connection error from " + ip + ": " + e.getMessage());
        } finally {
            if (!bridged) {
                active.decrementAndGet();
                Wire.closeQuiet(s);
            }
        }
    }
}
