package io.icker.factions.mixin;

import com.mojang.brigadier.CommandDispatcher;

import io.icker.factions.FactionsMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {
    @Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;

    // TODO delete... is this even used anymore?
    @Inject(at = @At("RETURN"), method = "<init>")
    private void onRegister(CommandManager.RegistrationEnvironment arg, CallbackInfo info) {
        FactionsMod.registerCommands(dispatcher);
    }
}