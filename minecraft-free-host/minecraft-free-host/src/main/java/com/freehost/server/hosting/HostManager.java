package com.freehost.server.hosting;

import com.freehost.common.NetLog;
import com.freehost.common.config.FreeHostConfig;
import com.freehost.common.protocol.Invite;
import com.freehost.server.players.PlayerRow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/** État global de l'hébergement (un seul monde hébergé à la fois). */
public final class HostManager {
    public enum State { OFFLINE, STARTING, ONLINE, ERROR }

    private static volatile State state = State.OFFLINE;
    private static volatile String message = "Not hosting";
    private static volatile String code = "";
    private static volatile long expiresAt = 0;
    private static volatile IntegratedServer server;
    private static volatile String hostName = "";
    private static volatile long pendingUntil = 0;

    private static Gateway gateway;
    private static UpnpHelper upnp;
    private static byte[] ip;
    private static int publicPort;

    private HostManager() {}

    // ---- lecture d'état ----
    public static State state() { return state; }
    public static String message() { return message; }
    public static String code() { return code; }
    public static long expiresAt() { return expiresAt; }
    public static String hostName() { return hostName; }

    public static boolean isFull() {
        IntegratedServer s = server;
        return s != null && s.getCurrentPlayerCount() >= FreeHostConfig.maxPlayers;
    }

    public static int playerCount() {
        IntegratedServer s = server;
        return s == null ? 0 : s.getCurrentPlayerCount();
    }

    // ---- démarrage automatique après avoir lancé un monde depuis "Host World" ----
    public static void requestHostOnJoin() { pendingUntil = System.currentTimeMillis() + 120_000; }

    public static boolean consumePending() {
        boolean p = System.currentTimeMillis() < pendingUntil;
        pendingUntil = 0;
        return p;
    }

    // ---- démarrage ----
    public static synchronized void start(MinecraftClient client) {
        if (state == State.STARTING || state == State.ONLINE) return;
        IntegratedServer srv = client.getServer();
        if (srv == null) {
            setError("Open a world first");
            return;
        }
        state = State.STARTING;
        message = "Opening world to network...";
        hostName = client.getSession().getUsername();
        server = srv;

        int mcPort;
        try {
            if (srv.isRemote()) {
                mcPort = srv.getServerPort();
            } else {
                mcPort = freePort();
                if (!srv.openToLan(srv.getDefaultGameMode(), false, mcPort)) {
                    setError("Could not open the world to network");
                    return;
                }
            }
        } catch (IOException e) {
            setError("No free local port");
            return;
        }
        final int fMcPort = mcPort;
        Thread.ofPlatform().name("freehost-start").daemon(true).start(() -> bringUp(fMcPort));
    }

    private static void bringUp(int mcPort) {
        try {
            publicPort = FreeHostConfig.gatewayPort;
            UpnpHelper helper = new UpnpHelper();
            upnp = helper;
            UpnpHelper.Result r = helper.open(publicPort);
            if (r.externalIp() == null) {
                setError("Could not find your public IP (no internet?)");
                return;
            }
            ip = InetAddress.getByName(r.externalIp()).getAddress();
            if (ip.length != 4) {
                setError("IPv6 is not supported yet");
                return;
            }
            byte[] secret = Invite.newSecret();
            long exp = newExpiry();
            Gateway g = new Gateway(publicPort, mcPort, secret, exp);
            g.start();
            synchronized (HostManager.class) {
                gateway = g;
                code = new Invite(ip, publicPort, secret).encode();
                expiresAt = exp;
                state = State.ONLINE;
                message = r.warning() == null ? "Online" : r.warning();
            }
            NetLog.add("Hosting online");
        } catch (java.net.BindException e) {
            setError("Port " + publicPort + " already in use (change it in Settings)");
        } catch (Exception e) {
            setError("Start failed: " + e.getMessage());
        }
    }

    public static synchronized void newCode() {
        if (state != State.ONLINE || gateway == null) return;
        byte[] secret = Invite.newSecret();
        long exp = newExpiry();
        gateway.update(secret, exp);
        code = new Invite(ip, publicPort, secret).encode();
        expiresAt = exp;
        NetLog.add("New invitation code generated");
    }

    private static long newExpiry() {
        return System.currentTimeMillis() + FreeHostConfig.expiryMinutes * 60_000L;
    }

    // ---- arrêt ----
    public static synchronized void stop(boolean kickGuests) {
        if (state == State.OFFLINE) return;
        if (kickGuests) kickAllGuests();
        cleanup();
        state = State.OFFLINE;
        message = "Not hosting";
        code = "";
        expiresAt = 0;
        NetLog.add("Hosting stopped");
    }

    /** Appelé à la fermeture du jeu (hook JVM) : ferme surtout le port UPnP. */
    public static void shutdown() {
        try {
            cleanup();
        } catch (Throwable ignored) {
        }
    }

    private static void cleanup() {
        if (gateway != null) {
            gateway.stop();
            gateway = null;
        }
        UpnpHelper u = upnp;
        upnp = null;
        if (u != null) u.close();
    }

    private static void setError(String msg) {
        cleanup();
        state = State.ERROR;
        message = msg;
        NetLog.add("ERROR: " + msg);
    }

    // ---- joueurs ----
    public static List<PlayerRow> players() {
        List<PlayerRow> rows = new ArrayList<>();
        IntegratedServer s = server;
        if (s == null) return rows;
        try {
            for (ServerPlayerEntity p : new ArrayList<>(s.getPlayerManager().getPlayerList())) {
                String name = p.getGameProfile().getName();
                if (!name.equals(hostName)) rows.add(new PlayerRow(p.getUuid(), name));
            }
        } catch (Exception ignored) {
        }
        return rows;
    }

    private static void kickAllGuests() {
        IntegratedServer s = server;
        if (s == null) return;
        try {
            s.execute(() -> {
                for (ServerPlayerEntity p : new ArrayList<>(s.getPlayerManager().getPlayerList())) {
                    if (!p.getGameProfile().getName().equals(hostName)) {
                        p.networkHandler.disconnect(net.minecraft.text.Text.literal("The host stopped hosting"));
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }
}
