package com.freehost.client.ui;

public final class UiTheme {
    public static final int PANEL = 0xFF14171F;
    public static final int BORDER = 0xFF2A2F3D;
    public static final int ACCENT = 0xFF7C5CFF;
    public static final int ACCENT_LIGHT = 0xFFC2B3FF;
    public static final int TEXT = 0xFFE6E8F0;
    public static final int MUTED = 0xFF8A90A3;
    public static final int OK = 0xFF3DDC84;
    public static final int BAD = 0xFFFF5370;
    public static final int WARN = 0xFFFFB454;

    private UiTheme() {}

    /** 0 -> 1 sur 250 ms (fondu à l'ouverture d'un écran). */
    public static float fade(long openedAtMs) {
        return Math.min(1f, (System.currentTimeMillis() - openedAtMs) / 250f);
    }

    /** Oscillation douce entre 0 et 1. */
    public static float pulse(double periodMs) {
        return (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / periodMs * Math.PI * 2));
    }

    public static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
