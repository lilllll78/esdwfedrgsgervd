package com.freehost.common.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class FreeHostConfig {
    public static volatile int maxPlayers = 8;
    public static volatile int expiryMinutes = 30;
    public static volatile int gatewayPort = 25566;

    private FreeHostConfig() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("freehost.properties");
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) {
            save();
            return;
        }
        try (InputStream in = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(in);
            maxPlayers = clamp(parse(props.getProperty("maxPlayers"), 8), 2, 20);
            expiryMinutes = clamp(parse(props.getProperty("expiryMinutes"), 30), 1, 1440);
            gatewayPort = clamp(parse(props.getProperty("gatewayPort"), 25566), 1024, 65535);
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("maxPlayers", String.valueOf(maxPlayers));
        props.setProperty("expiryMinutes", String.valueOf(expiryMinutes));
        props.setProperty("gatewayPort", String.valueOf(gatewayPort));
        try (OutputStream out = Files.newOutputStream(path())) {
            props.store(out, "FreeHost settings");
        } catch (IOException ignored) {
        }
    }

    private static int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
