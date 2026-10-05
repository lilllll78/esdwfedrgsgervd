package com.freehost.client.screens;

import com.freehost.client.networking.GuestProxy;
import com.freehost.client.networking.InviteClient;
import com.freehost.client.ui.UiTheme;
import com.freehost.common.protocol.Invite;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.io.IOException;

public class JoinScreen extends BaseScreen {
    private TextFieldWidget field;
    private ButtonWidget joinButton;
    private String saved = "";
    private String status = "";
    private int statusColor = UiTheme.MUTED;
    private boolean busy = false;

    public JoinScreen(Screen parent) {
        super("Join World", parent);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        field = new TextFieldWidget(textRenderer, cx - 120, cy - 20, 240, 20, Text.literal("Invitation code"));
        field.setMaxLength(48);
        field.setPlaceholder(Text.literal("XXXX-XXXX-XXXX-XXXX-XXXX-XXX"));
        field.setText(saved);
        field.setChangedListener(s -> saved = s);
        addDrawableChild(field);
        setInitialFocus(field);

        joinButton = btn("JOIN", cx - 100, cy + 8, 200, 20, b -> join());
        joinButton.active = !busy;
        addDrawableChild(joinButton);
        addDrawableChild(btn("BACK", cx - 100, cy + 56, 200, 20, b -> close()));
    }

    private void setStatus(String s, int color) {
        status = s;
        statusColor = color;
    }

    private void join() {
        Invite inv;
        try {
            inv = Invite.decode(field.getText());
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage(), UiTheme.BAD);
            return;
        }
        busy = true;
        joinButton.active = false;
        setStatus("Connecting...", UiTheme.WARN);
        final Invite invite = inv;
        Thread.ofVirtual().start(() -> {
            InviteClient.Result r = InviteClient.check(invite);
            client.execute(() -> {
                busy = false;
                if (joinButton != null) joinButton.active = true;
                if (!r.ok()) {
                    setStatus(r.message(), UiTheme.BAD);
                    return;
                }
                setStatus("Host found (" + r.pingMs() + " ms) - joining...", UiTheme.OK);
                try {
                    int port = GuestProxy.start(invite);
                    String addr = "127.0.0.1:" + port;
                    ConnectScreen.connect(this, client, ServerAddress.parse(addr),
                            new ServerInfo("FreeHost World", addr, ServerInfo.ServerType.OTHER), false, null);
                } catch (IOException e) {
                    setStatus("Local proxy error: " + e.getMessage(), UiTheme.BAD);
                }
            });
        });
    }

    @Override
    protected void drawPanel(DrawContext ctx) {
        int cx = width / 2;
        int cy = height / 2;
        ctx.fill(cx - 130, cy - 60, cx + 130, cy + 84, UiTheme.PANEL);
        ctx.drawBorder(cx - 130, cy - 60, 260, 144, UiTheme.BORDER);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cy = height / 2;
        drawTitle(ctx, "JOIN WORLD", cy - 52);
        centered(ctx, "Enter invitation code:", cy - 34, UiTheme.MUTED);
        if (!status.isEmpty()) {
            centered(ctx, "Status: " + status, cy + 36, statusColor);
        }
    }
}
