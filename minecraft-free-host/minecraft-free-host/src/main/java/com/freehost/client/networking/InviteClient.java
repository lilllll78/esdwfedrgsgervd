package com.freehost.client.networking;

import com.freehost.common.protocol.Invite;
import com.freehost.common.protocol.Wire;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/** Vérifie un code auprès de l'hôte avant de lancer la connexion Minecraft (et mesure la latence). */
public final class InviteClient {
    public record Result(boolean ok, String message, long pingMs) {}

    private InviteClient() {}

    public static Result check(Invite inv) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByAddress(inv.ip()), inv.port()), 5000);
            s.setSoTimeout(5000);
            long t = System.nanoTime();
            Wire.writeLine(s.getOutputStream(), Wire.HELLO + " " + Wire.hex(inv.secret()) + " CHECK");
            String reply = Wire.readLine(s.getInputStream(), 64);
            long ping = (System.nanoTime() - t) / 1_000_000;
            return switch (reply) {
                case "OK" -> new Result(true, "OK", ping);
                case "ERR EXPIRED" -> new Result(false, "This code has expired", ping);
                case "ERR FULL" -> new Result(false, "The world is full", ping);
                default -> new Result(false, "Invalid or revoked code", ping);
            };
        } catch (SocketTimeoutException e) {
            return new Result(false, "Host did not answer (timeout)", -1);
        } catch (IOException e) {
            return new Result(false, "Host unreachable (offline, port closed or CGNAT)", -1);
        }
    }
}
