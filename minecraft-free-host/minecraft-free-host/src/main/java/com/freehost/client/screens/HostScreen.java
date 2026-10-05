package com.freehost.client.screens;

import com.freehost.client.ui.UiTheme;
import com.freehost.common.NetLog;
import com.freehost.common.config.FreeHostConfig;
import com.freehost.server.hosting.HostManager;
import com.freehost.server.players.PlayerActions;
import com.freehost.server.players.PlayerRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class HostScreen extends BaseScreen {
    private static final int MAX_ROWS = 3;
    private String lastKey = "";
    private List<PlayerRow> rows = List.of();

    public HostScreen(Screen parent) {
        super("World Hosting", parent);
    }

    private String key() {
        StringBuilder sb = new StringBuilder(HostManager.state().name());
        for (PlayerRow r : HostManager.players()) sb.append('|').append(r.name());
        return sb.toString();
    }

    @Override
    protected void init() {
        lastKey = key();
        rows = HostManager.players();
        int cx = width / 2;
        HostManager.State st = HostManager.state();

        if (st == HostManager.State.ONLINE) {
            int by = height - 48;
            addDrawableChild(btn("COPY CODE", cx - 148, by, 96, 20, b -> {
                client.keyboard.setClipboard(HostManager.code());
                NetLog.add("Code copied to clipboard");
            }));
            addDrawableChild(btn("NEW CODE", cx - 48, by, 96, 20, b -> HostManager.newCode()));
            addDrawableChild(btn("INVITE", cx + 52, by, 96, 20, b -> {
                client.keyboard.setClipboard("Join my Minecraft world! Open FreeHost > Join World and enter this code: " + HostManager.code());
                NetLog.add("Invitation message copied to clipboard");
            }));
            addDrawableChild(btn("STOP HOSTING", cx - 100, height - 24, 98, 20, b -> HostManager.stop(true)));
            addDrawableChild(btn("BACK", cx + 2, height - 24, 98, 20, b -> close()));

            for (int i = 0; i < Math.min(MAX_ROWS, rows.size()); i++) {
                final PlayerRow row = rows.get(i);
                int y = 98 + i * 14;
                addDrawableChild(btn("Kick", cx + 40, y, 34, 12, b -> PlayerActions.kick(client, row.uuid())));
                addDrawableChild(btn("Ban", cx + 78, y, 34, 12, b -> PlayerActions.ban(client, row.uuid())));
            }
        } else {
            ButtonWidget start = btn("START HOSTING", cx - 100, height - 48, 200, 20, b -> HostManager.start(client));
            start.active = client.isIntegratedServerRunning() && st != HostManager.State.STARTING;
            addDrawableChild(start);
            addDrawableChild(btn("BACK", cx - 100, height - 24, 200, 20, b -> close()));
        }
    }

    @Override
    public void tick() {
        if (!key().equals(lastKey)) clearAndInit();
    }

    @Override
    protected void drawPanel(DrawContext ctx) {
        int cx = width / 2;
        ctx.fill(cx - 150, 4, cx + 150, height - 52 + 0, UiTheme.PANEL);
        ctx.drawBorder(cx - 150, 4, 300, height - 56, UiTheme.BORDER);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = width / 2;
        drawTitle(ctx, "WORLD HOSTING", 10);

        HostManager.State st = HostManager.state();
        int dot = switch (st) {
            case ONLINE -> UiTheme.lerp(UiTheme.OK, 0xFF1E7A48, UiTheme.pulse(1800));
            case STARTING -> UiTheme.WARN;
            case ERROR -> UiTheme.BAD;
            default -> UiTheme.MUTED;
        };
        String label = switch (st) {
            case ONLINE -> "Online";
            case STARTING -> "Starting...";
            case ERROR -> "Error";
            default -> "Offline";
        };
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u25CF Status: " + label), cx, 24, dot);

        if (st == HostManager.State.ONLINE) {
            centered(ctx, "Join Code", 36, UiTheme.MUTED);
            ctx.getMatrices().push();
            ctx.getMatrices().scale(1.5f, 1.5f, 1f);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(HostManager.code()),
                    (int) (cx / 1.5f), (int) (47 / 1.5f), UiTheme.TEXT);
            ctx.getMatrices().pop();

            long exp = HostManager.expiresAt();
            if (exp > 0) {
                long left = (exp - System.currentTimeMillis()) / 1000;
                if (left <= 0) centered(ctx, "Code expired - press NEW CODE", 66, UiTheme.BAD);
                else centered(ctx, String.format("Expires in %d:%02d", left / 60, left % 60), 66, UiTheme.MUTED);
            }
            String msg = HostManager.message();
            if (!msg.equals("Online")) centered(ctx, msg, 76, UiTheme.WARN);

            centered(ctx, "Players: " + HostManager.playerCount() + " / " + FreeHostConfig.maxPlayers, 86, UiTheme.TEXT);
            for (int i = 0; i < Math.min(MAX_ROWS, rows.size()); i++) {
                ctx.drawTextWithShadow(textRenderer, rows.get(i).name(), cx - 110, 100 + i * 14, UiTheme.TEXT);
            }
            if (rows.size() > MAX_ROWS) {
                ctx.drawTextWithShadow(textRenderer, "+" + (rows.size() - MAX_ROWS) + " more", cx - 110, 100 + MAX_ROWS * 14, UiTheme.MUTED);
            }
        } else if (st == HostManager.State.ERROR) {
            centered(ctx, HostManager.message(), 44, UiTheme.BAD);
        } else if (st == HostManager.State.STARTING) {
            centered(ctx, HostManager.message(), 44, UiTheme.MUTED);
        } else if (!client.isIntegratedServerRunning()) {
            centered(ctx, "Open a world first (use Host World from the menu)", 44, UiTheme.MUTED);
        } else {
            centered(ctx, "Your world is ready to be shared", 44, UiTheme.MUTED);
        }

        List<String> logs = NetLog.last(3);
        int ly = height - 52 - 30;
        for (int i = 0; i < logs.size(); i++) {
            String line = logs.get(i);
            if (textRenderer.getWidth(line) > 280) line = textRenderer.trimToWidth(line, 280);
            ctx.drawTextWithShadow(textRenderer, line, cx - 144, ly + i * 9, 0xFF5F6578);
        }
    }
}
