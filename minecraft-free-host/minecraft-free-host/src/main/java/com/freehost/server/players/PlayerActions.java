package com.freehost.server.players;

import com.freehost.common.NetLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class PlayerActions {
    private PlayerActions() {}

    public static void kick(MinecraftClient client, UUID uuid) {
        IntegratedServer s = client.getServer();
        if (s == null) return;
        s.execute(() -> {
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(uuid);
            if (p != null) {
                NetLog.add("Kicked " + p.getGameProfile().getName());
                p.networkHandler.disconnect(Text.literal("You were kicked by the host"));
            }
        });
    }

    public static void ban(MinecraftClient client, UUID uuid) {
        IntegratedServer s = client.getServer();
        if (s == null) return;
        s.execute(() -> {
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(uuid);
            if (p != null) {
                NetLog.add("Banned " + p.getGameProfile().getName());
                s.getPlayerManager().getUserBanList().add(new BannedPlayerEntry(p.getGameProfile()));
                p.networkHandler.disconnect(Text.literal("You were banned by the host"));
            }
        });
    }
}
