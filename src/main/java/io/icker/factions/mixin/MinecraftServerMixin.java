package io.icker.factions.mixin;

import io.icker.factions.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setFavicon(Lnet/minecraft/server/ServerMetadata;)V", ordinal = 0), method = "runServer")
	private void serverStarted(CallbackInfo info) {
		ServerEvents.started((MinecraftServer) (Object) this);
	}

	@Inject(at = @At("TAIL"), method = "shutdown")
	private void serverStopped(CallbackInfo info) {
		ServerEvents.stopped((MinecraftServer) (Object) this);
	}
}