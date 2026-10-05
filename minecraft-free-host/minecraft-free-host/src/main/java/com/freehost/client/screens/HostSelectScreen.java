package com.freehost.client.screens;

import com.freehost.client.ui.UiTheme;
import com.freehost.server.hosting.HostManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.world.level.storage.LevelSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HostSelectScreen extends BaseScreen {
    private static final int PER_PAGE = 5;
    private List<LevelSummary> worlds = new ArrayList<>();
    private boolean loadStarted = false;
    private boolean loaded = false;
    private String error = null;
    private int selected = -1;
    private int page = 0;

    public HostSelectScreen(Screen parent) {
        super("Select World", parent);
    }

    @Override
    protected void init() {
        if (!loadStarted) {
            loadStarted = true;
            try {
                var storage = client.getLevelStorage();
                var list = storage.getLevelList();
                storage.loadSummaries(list).thenAccept(result -> client.execute(() -> {
                    worlds = new ArrayList<>(result);
                    worlds.sort(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed());
                    loaded = true;
                    clearAndInit();
                })).exceptionally(t -> {
                    client.execute(() -> {
                        error = "Could not read worlds";
                        loaded = true;
                    });
                    return null;
                });
            } catch (Exception e) {
                error = "Could not read worlds";
                loaded = true;
            }
        }

        int cx = width / 2;
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < worlds.size(); i++) {
            final int idx = start + i;
            String label = (idx == selected ? "> " : "") + worlds.get(idx).getDisplayName();
            addDrawableChild(btn(label, cx - 110, 30 + i * 22, 220, 20, b -> {
                selected = idx;
                clearAndInit();
            }));
        }

        ButtonWidget prev = btn("<", cx - 110, 144, 30, 18, b -> {
            page--;
            clearAndInit();
        });
        prev.active = page > 0;
        addDrawableChild(prev);
        ButtonWidget next = btn(">", cx + 80, 144, 30, 18, b -> {
            page++;
            clearAndInit();
        });
        next.active = (page + 1) * PER_PAGE < worlds.size();
        addDrawableChild(next);

        ButtonWidget host = btn("HOST", cx - 100, height - 46, 200, 20, b -> {
            LevelSummary w = worlds.get(selected);
            HostManager.requestHostOnJoin();
            client.createIntegratedServerLoader().start(this, w.getName());
        });
        host.active = selected >= 0 && selected < worlds.size();
        addDrawableChild(host);

        addDrawableChild(btn("BACK", cx - 100, height - 24, 200, 20, b -> close()));
    }

    @Override
    protected void drawPanel(DrawContext ctx) {
        int cx = width / 2;
        ctx.fill(cx - 125, 4, cx + 125, 168, UiTheme.PANEL);
        ctx.drawBorder(cx - 125, 4, 250, 164, UiTheme.BORDER);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawTitle(ctx, "SELECT WORLD", 12);
        if (!loaded) {
            centered(ctx, "Loading worlds...", 70, UiTheme.MUTED);
        } else if (error != null) {
            centered(ctx, error, 70, UiTheme.BAD);
        } else if (worlds.isEmpty()) {
            centered(ctx, "No world found. Create one first.", 70, UiTheme.MUTED);
        } else {
            int pages = Math.max(1, (worlds.size() + PER_PAGE - 1) / PER_PAGE);
            centered(ctx, (page + 1) + " / " + pages, 149, UiTheme.MUTED);
        }
    }
}
