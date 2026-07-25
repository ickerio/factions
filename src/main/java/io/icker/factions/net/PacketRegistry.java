package io.icker.factions.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PacketRegistry {
    private PacketRegistry() {}

    /** Call ONCE from the common ModInitializer. PayloadTypeRegistry is shared across sides. */
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ClaimSyncPayload.TYPE, ClaimSyncPayload.CODEC);
    }
}
