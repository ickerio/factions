package io.icker.factions.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class InputGui extends AnvilInputGui {
    public GuiElementBuilder returnBtn;
    public GuiElementBuilder confirmBtn;
    private final Timer timer = new Timer();

    public InputGui(ServerPlayerEntity player) {
        super(player, false);

        this.returnBtn = new GuiElementBuilder(Items.BARRIER)
                .setName(Text.translatable("gui.generic.back")
                        .formatted(Formatting.RED));
        this.confirmBtn = new GuiElementBuilder(Items.SLIME_BALL)
                .setName(Text.translatable("gui.generic.confirm")
                        .formatted(Formatting.GREEN));
    }

    public void showErrorMessage(Text text, int slotIndex) {
        ItemStack item = Objects.requireNonNull(this.getSlot(slotIndex)).getItemStack();
        item.set(DataComponentTypes.CUSTOM_NAME, text);
        player.playSoundToPlayer(SoundEvent.of(Identifier.of("minecraft:item.shield.break")), SoundCategory.BLOCKS, 1, 1);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                item.remove(DataComponentTypes.CUSTOM_NAME);
            }
        }, 1500);
    }

    @Override
    public boolean open() {
        this.setSlot(1, returnBtn);
        this.setSlot(2, confirmBtn);
        return super.open();
    }
}
