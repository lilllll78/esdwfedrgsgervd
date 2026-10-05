package com.freehost.client.screens;

import com.freehost.client.ui.UiTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

public class MainMenuScreen extends BaseScreen {
    public MainMenuScreen(Screen parent) {
        super("World Hosting", parent);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 32;
        boolean inGuestWorld = client.world != null && !client.isIntegratedServerRunning();
        boolean inAnyWorld = client.world != null;

        ButtonWidget host = btn("HOST WORLD", cx - 100, y, 200, 22, b ->
                client.setScreen(client.isIntegratedServerRunning() ? new HostScreen(this) : new HostSelectScreen(this)));
        host.active = !inGuestWorld;
        addDrawableChild(host);

        ButtonWidget join = btn("JOIN WORLD", cx - 100, y + 28, 200, 22, b -> client.setScreen(new JoinScreen(this)));
        join.active = !inAnyWorld;
        addDrawableChild(join);

        addDrawableChild(btn("SETTINGS", cx - 100, y + 56, 200, 22, b -> client.setScreen(new SettingsScreen(this))));
        addDrawableChild(btn("BACK", cx - 100, y + 90, 200, 20, b -> close()));
    }

    @Override
    protected void drawPanel(DrawContext ctx) {
        int cx = width / 2;
        int top = height / 2 - 70;
        ctx.fill(cx - 120, top, cx + 120, top + 190, UiTheme.PANEL);
        ctx.drawBorder(cx - 120, top, 240, 190, UiTheme.BORDER);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawTitle(ctx, "WORLD HOSTING", height / 2 - 58);
        if (client.world != null && !client.isIntegratedServerRunning()) {
            centered(ctx, "Leave the current server to host or join", height / 2 + 66, UiTheme.MUTED);
        } else if (client.world != null) {
            centered(ctx, "Leave your world to join another one", height / 2 + 66, UiTheme.MUTED);
        }
    }
}
