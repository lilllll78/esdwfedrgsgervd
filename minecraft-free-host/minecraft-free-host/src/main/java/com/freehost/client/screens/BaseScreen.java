package com.freehost.client.screens;

import com.freehost.client.ui.UiTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Base commune : fond sombre, panneau, titre animé, fondu à l'ouverture. */
public abstract class BaseScreen extends Screen {
    protected final Screen parent;
    private final long openedAt = System.currentTimeMillis();

    protected BaseScreen(String title, Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xFF0B0D12, 0xFF171326);
        drawPanel(ctx);
    }

    protected void drawPanel(DrawContext ctx) {}

    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawContent(ctx, mouseX, mouseY, delta);
        float f = UiTheme.fade(openedAt);
        if (f < 1f) {
            int a = (int) ((1f - f) * 255);
            if (a > 0) ctx.fill(0, 0, width, height, a << 24);
        }
    }

    protected void drawTitle(DrawContext ctx, String text, int y) {
        int color = UiTheme.lerp(UiTheme.ACCENT, UiTheme.ACCENT_LIGHT, UiTheme.pulse(2500));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(text).formatted(Formatting.BOLD), width / 2, y, color);
    }

    protected void centered(DrawContext ctx, String text, int y, int color) {
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(text), width / 2, y, color);
    }

    protected static ButtonWidget btn(String label, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, w, h).build();
    }
}
