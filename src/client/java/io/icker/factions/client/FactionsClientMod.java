package io.icker.factions.client;

import io.icker.factions.net.ClaimSyncPayload;
import io.icker.factions.client.xaero.XaeroWorldMapWrapper;
import io.icker.factions.client.xaero.XaeroMinimapWrapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class FactionsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                ClaimSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClaimCache.update(payload)));

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClaimCache.clear());

        if (FabricLoader.getInstance().isModLoaded("xaeroworldmap")) {
            XaeroWorldMapWrapper.register();
        }
        if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            XaeroMinimapWrapper.register();
        }
    }
}
