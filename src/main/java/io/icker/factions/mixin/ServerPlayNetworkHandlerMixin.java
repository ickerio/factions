package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.icker.factions.api.events.PlayerEvents;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        PlayerEvents.ON_MOVE.invoker().onMove(player);
    }

    @Redirect(method = "handleDecoratedMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/server/filter/FilteredMessage;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/registry/RegistryKey;)V"))
    private void replaceChatMessage(PlayerManager instance, FilteredMessage<SignedMessage> message, ServerPlayerEntity sender, RegistryKey<MessageType> typeKey) {
        User member = User.get(sender.getUuid());
        Faction faction = member != null ? member.getFaction() : null;

        boolean factionChat = member.chat == User.ChatMode.FACTION || member.chat == User.ChatMode.FOCUS;

        if (factionChat && faction == null) {
            new Message("You can't send a message to faction chat if you aren't in a faction").fail().hover("Click to switch to global chat").click("/factions chat global").send(sender, false);
        } else {
            instance.broadcast(message.raw(), player -> {
                User targetMember = User.get(player.getUuid());
                Faction target = targetMember != null ? targetMember.getFaction() : null;

                if (member.chat == User.ChatMode.GLOBAL && targetMember.chat != User.ChatMode.FOCUS) {
                    return message.getFilterableFor(sender, player);
                }

                if (factionChat && target != null && target.getName().equals(faction.getName())) {
                    return message.getFilterableFor(sender, player);
                }

                return null;
            }, sender.asMessageSender(), typeKey);
        }
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"))
    public void onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        Faction faction = User.get(player.getUuid()).getFaction();
        FactionsMod.LOGGER.info(packet.getSyncId());

        if (faction.syncId == packet.getSyncId() && packet.getRevision() == faction.currentRevision) {
            EnderChestInventory safe = faction.getSafe();
            faction.currentRevision++;

            Int2ObjectMaps.fastForEach(packet.getModifiedStacks(), entry -> {
                if (entry.getIntKey() < 27) {
                    safe.setStack(entry.getIntKey(), entry.getValue());
                }
            });

            faction.setSafe(safe);

            DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);

            for (int i = 0; i < 27; i++) {
                items.set(i, safe.getStack(i));
            }

            PlayerManager manager = player.getServer().getPlayerManager();
            for (User user : faction.getUsers()) {
                ServerPlayerEntity currentPlayer;
                if ((currentPlayer = manager.getPlayer(user.getID())) != null) {
                    currentPlayer.networkHandler.sendPacket(new InventoryS2CPacket(faction.syncId, faction.currentRevision, items, ItemStack.EMPTY));
                }
            }
        } else if (faction.syncId == packet.getSyncId()) {
            DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
            EnderChestInventory safe = faction.getSafe();

            for (int i = 0; i < 27; i++) {
                items.set(i, safe.getStack(i));
            }

            player.networkHandler.sendPacket(new InventoryS2CPacket(faction.syncId, faction.currentRevision, items, ItemStack.EMPTY));
        }
    }
}
