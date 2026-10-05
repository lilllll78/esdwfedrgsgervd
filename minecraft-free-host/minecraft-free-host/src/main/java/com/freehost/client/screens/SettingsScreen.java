package com.freehost.client.screens;

import com.freehost.client.ui.UiTheme;
import com.freehost.common.config.FreeHostConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends BaseScreen {
    private static final int[] MAX_PLAYERS = {2, 4, 8, 12, 16, 20};
    private static final int[] EXPIRY = {10, 30, 60, 240};
    private TextFieldWidget portField;
    private String portText = String.valueOf(FreeHostConfig.gatewayPort);

    public SettingsScreen(Screen parent) {
        super("Settings", parent);
    }

    private static int next(int[] a, int cur) {
        for (int i = 0; i < a.length; i++) if (a[i] == cur) return a[(i + 1) % a.length];
        return a[0];
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        addDrawableChild(btn("Max players: " + FreeHostConfig.maxPlayers, cx - 100, cy - 34, 200, 20, b -> {
            FreeHostConfig.maxPlayers = next(MAX_PLAYERS, FreeHostConfig.maxPlayers);
            clearAndInit();
        }));
        addDrawableChild(btn("Code expiry: " + FreeHostConfig.expiryMinutes + " min", cx - 100, cy - 10, 200, 20, b -> {
            FreeHostConfig.expiryMinutes = next(EXPIRY, FreeHostConfig.expiryMinutes);
            clearAndInit();
        }));
        portField = new TextFieldWidget(textRenderer, cx - 100, cy + 24, 200, 20, Text.literal("Port"));
        portField.setMaxLength(5);
        portField.setTextPredicate(s -> s.matches("\\d{0,5}"));
        portField.setText(portText);
        portField.setChangedListener(s -> portText = s);
        addDrawableChild(portField);
        addDrawableChild(btn("DONE", cx - 100, cy + 56, 200, 20, b -> close()));
    }

    @Override
    public void close() {
        try {
            int p = Integer.parseInt(portText);
            if (p >= 1024 && p <= 65535) FreeHostConfig.gatewayPort = p;
        } catch (NumberFormatException ignored) {
        }
        FreeHostConfig.save();
        super.close();
    }

    @Override
    protected void drawPanel(DrawContext ctx) {
        int cx = width / 2;
        int cy = height / 2;
        ctx.fill(cx - 120, cy - 74, cx + 120, cy + 84, UiTheme.PANEL);
        ctx.drawBorder(cx - 120, cy - 74, 240, 158, UiTheme.BORDER);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cy = height / 2;
        drawTitle(ctx, "SETTINGS", cy - 62);
        centered(ctx, "Network port (TCP, 1024-65535)", cy + 12, UiTheme.MUTED);
    }
}
