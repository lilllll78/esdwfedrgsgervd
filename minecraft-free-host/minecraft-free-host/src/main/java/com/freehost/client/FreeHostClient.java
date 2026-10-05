package com.freehost.client;

import com.freehost.client.networking.GuestProxy;
import com.freehost.client.screens.MainMenuScreen;
import com.freehost.common.config.FreeHostConfig;
import com.freehost.server.hosting.HostManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class FreeHostClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FreeHostConfig.load();

        // Bouton "World Hosting" en haut à gauche du menu principal et du menu pause
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen || screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(
                        ButtonWidget.builder(Text.literal("World Hosting"), b -> client.setScreen(new MainMenuScreen(screen)))
                                .dimensions(4, 4, 100, 20).build());
            }
        });

        // Monde lancé depuis "Host World" : on démarre l'hébergement dès qu'on est dedans
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (HostManager.consumePending()) HostManager.start(client);
        });

        // Fermeture du monde / déconnexion : arrêt propre
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            HostManager.stop(false);
            GuestProxy.stop();
        });

        // Limite de joueurs (le serveur solo en autorise 8 par défaut, on applique la config)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (HostManager.state() == HostManager.State.ONLINE
                    && server.getCurrentPlayerCount() > FreeHostConfig.maxPlayers) {
                handler.disconnect(Text.literal("The world is full"));
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> HostManager.shutdown());
        Runtime.getRuntime().addShutdownHook(new Thread(HostManager::shutdown));
    }
}
