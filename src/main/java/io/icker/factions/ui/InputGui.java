package io.icker.factions.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;

import io.icker.factions.util.GuiInteract;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class InputGui extends AnvilInputGui {
    public GuiElementBuilder returnBtn;
    public GuiElementBuilder confirmBtn;
    private final Timer timer = new Timer();

    public InputGui(ServerPlayer player) {
        super(player, false);

        this.returnBtn =
                new GuiElementBuilder(Items.BARRIER)
                        .setName(
                                Component.translatable("factions.gui.generic.back")
                                        .withStyle(ChatFormatting.RED));
        this.confirmBtn =
                new GuiElementBuilder(Items.SLIME_BALL)
                        .setName(
                                Component.translatable("factions.gui.generic.confirm")
                                        .withStyle(ChatFormatting.GREEN));
    }

    public void showErrorMessage(String text, int slotIndex) {
        showErrorMessage(Component.literal(text), slotIndex);
    }

    public void showErrorMessage(MutableComponent text, int slotIndex) {
        ItemStack item = Objects.requireNonNull(this.getSlot(slotIndex)).getItemStack();
        item.set(
                DataComponents.CUSTOM_NAME,
                text.setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.RED)));
        GuiInteract.playSound(
                player,
                SoundEvent.createVariableRangeEvent(
                        Identifier.parse("minecraft:item.shield.break")),
                1f,
                1f);
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        item.remove(DataComponents.CUSTOM_NAME);
                    }
                },
                1500);
    }

    @Override
    public boolean open() {
        this.setSlot(1, returnBtn);
        this.setSlot(2, confirmBtn);
        return super.open();
    }
}
