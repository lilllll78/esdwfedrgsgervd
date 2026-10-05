package com.freehost.common.protocol;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Mini protocole de poignée de main : "FH1 <secret hex> <CHECK|JOIN>\n" -> "OK\n" ou "ERR <raison>\n". */
public final class Wire {
    public static final String HELLO = "FH1";

    private Wire() {}

    public static String readLine(InputStream in, int max) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return sb.toString();
            if (c == '\r') continue;
            if (sb.length() >= max) throw new IOException("line too long");
            sb.append((char) c);
        }
        throw new IOException("connection closed");
    }

    public static void writeLine(OutputStream out, String s) throws IOException {
        out.write((s + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    public static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /** @return null si invalide */
    public static byte[] unhex(String s) {
        if (s == null || s.length() % 2 != 0 || s.length() > 64) return null;
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(s.charAt(2 * i), 16);
            int lo = Character.digit(s.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static void closeQuiet(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }

    /** Relie deux sockets dans les deux sens ; ferme tout dès qu'un côté se termine. */
    public static void bridge(Socket a, Socket b, Runnable onDone) {
        AtomicInteger remaining = new AtomicInteger(2);
        Runnable done = () -> {
            if (remaining.decrementAndGet() == 0 && onDone != null) onDone.run();
        };
        Thread.ofVirtual().start(() -> copy(a, b, done));
        Thread.ofVirtual().start(() -> copy(b, a, done));
    }

    private static void copy(Socket from, Socket to, Runnable done) {
        try {
            byte[] buf = new byte[16384];
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {
        } finally {
            closeQuiet(from);
            closeQuiet(to);
            done.run();
        }
    }
}
