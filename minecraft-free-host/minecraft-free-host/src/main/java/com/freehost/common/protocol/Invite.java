package com.freehost.common.protocol;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.io.ByteArrayOutputStream;
import java.util.zip.CRC32;

/**
 * Code d'invitation = IPv4 (4 octets) + port (2) + secret aléatoire (7) + checksum (1)
 * encodé en base32 (sans I, O, 0, 1) : 23 caractères, groupés par 4.
 * Le secret fait 56 bits, généré avec SecureRandom.
 */
public record Invite(byte[] ip, int port, byte[] secret) {
    public static final int SECRET_LEN = 7;
    private static final int RAW_LEN = 14;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    public static byte[] newSecret() {
        byte[] s = new byte[SECRET_LEN];
        RNG.nextBytes(s);
        return s;
    }

    public String host() {
        try {
            return InetAddress.getByAddress(ip).getHostAddress();
        } catch (Exception e) {
            return "?";
        }
    }

    public String encode() {
        byte[] raw = new byte[RAW_LEN];
        System.arraycopy(ip, 0, raw, 0, 4);
        raw[4] = (byte) (port >> 8);
        raw[5] = (byte) port;
        System.arraycopy(secret, 0, raw, 6, SECRET_LEN);
        raw[13] = checksum(raw);

        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : raw) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
            buffer &= (1 << bits) - 1;
        }
        if (bits > 0) sb.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));

        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < sb.length(); i++) {
            if (i > 0 && i % 4 == 0) grouped.append('-');
            grouped.append(sb.charAt(i));
        }
        return grouped.toString();
    }

    public static Invite decode(String text) {
        if (text == null) throw new IllegalArgumentException("Empty code");
        String clean = text.toUpperCase().replace("-", "").replace(" ", "").trim();
        if (clean.length() != 23) throw new IllegalArgumentException("Invalid code format");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (int i = 0; i < clean.length(); i++) {
            int idx = ALPHABET.indexOf(clean.charAt(i));
            if (idx < 0) throw new IllegalArgumentException("Invalid code format");
            buffer = (buffer << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
                buffer &= (1 << bits) - 1;
            }
        }
        byte[] raw = out.toByteArray();
        if (raw.length < RAW_LEN) throw new IllegalArgumentException("Invalid code format");
        if (raw[13] != checksum(raw)) throw new IllegalArgumentException("Invalid code (typo?)");

        byte[] ip = new byte[4];
        System.arraycopy(raw, 0, ip, 0, 4);
        int port = ((raw[4] & 0xFF) << 8) | (raw[5] & 0xFF);
        byte[] secret = new byte[SECRET_LEN];
        System.arraycopy(raw, 6, secret, 0, SECRET_LEN);
        if (port < 1024) throw new IllegalArgumentException("Invalid code format");
        return new Invite(ip, port, secret);
    }

    private static byte checksum(byte[] raw) {
        CRC32 crc = new CRC32();
        crc.update(raw, 0, 13);
        return (byte) crc.getValue();
    }
}
