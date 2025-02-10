package io.icker.factions.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.GuiInteract;
import io.icker.factions.util.Icons;
import io.icker.factions.util.Message;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ModifyGui extends SimpleGui {
    Runnable closeCallback;
    private final Runnable defaultReturn = () -> {
        GuiInteract.playClickSound(player);
        this.open();
    };

    public ModifyGui(ServerPlayerEntity player, Faction faction, @Nullable Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        this.closeCallback = closeCallback;

        this.setTitle(Text.literal("Faction settings"));
        for (int i = 0; i < 9; i++)
            this.setSlot(i, new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());

        this.setSlot(0, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_TV_TEXT)
                .setName(Text.literal("Change name"))
                .setCallback((index, clickType, actionType) -> {
                    this.execName(faction);
                })
        );
        this.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_BOOK)
                .setName(Text.literal("Change description"))
                .setCallback((index, clickType, actionType) -> {
                    this.execDesc(faction);
                })
        );
        this.setSlot(2, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_RADIO)
                .setName(Text.literal("Change MOTD"))
                .setCallback((index, clickType, actionType) -> {
                    this.execMOTD(faction);
                })
        );
        this.setSlot(4, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_PAINT_BUCKET)
                .setName(Text.literal("Change color"))
                .setCallback((index, clickType, actionType) -> {
                    new ColorGui(player, faction, this::open);
                })
        );
        this.setSlot(5, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(faction.isOpen() ? Icons.GUI_TESSERACT_BLUE : Icons.GUI_TESSERACT_RED)
                .setName(Text.literal("Faction type: ")
                        .append(faction.isOpen() ?
                                Text.literal("Public").formatted(Formatting.AQUA) :
                                Text.literal("Invite only").formatted(Formatting.RED)
                        )
                )
                .setCallback((index, clickType, actionType) -> {
                    faction.setOpen(!faction.isOpen());
                    ItemStack item = this.getSlot(index).getItemStack();

                    PropertyMap map = new PropertyMap();
                    map.put("textures", new Property("textures", faction.isOpen() ? Icons.GUI_TESSERACT_BLUE : Icons.GUI_TESSERACT_OFF, null));
                    item.set(DataComponentTypes.PROFILE, new ProfileComponent(Optional.empty(), Optional.empty(), map));

                    item.set(DataComponentTypes.ITEM_NAME,
                            Text.literal("Faction type: ").append(
                                    faction.isOpen() ?
                                            Text.literal("Public").formatted(Formatting.GREEN) :
                                            Text.literal("Invite only").formatted(Formatting.RED)
                            )
                    );
                })
        );

        this.setSlot(8, new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Text.literal(closeCallback == null ? "Close" : "Go back").formatted(Formatting.RED))
                .setCallback(closeCallback == null ? (Runnable) this::close : (Runnable) closeCallback)
        );

        this.open();
    }

    private void execName(Faction faction) {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(Text.literal("Specify a name..."));
        inputGui.setDefaultInputValue("New Faction Name");

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    String name = inputGui.getInput();
                    faction.setName(name);
                    new Message("Successfully renamed faction to '" + name + "'").prependFaction(faction)
                            .send(player, false);
                    this.open();
                }
        );

        inputGui.open();
    }

    private void execDesc(Faction faction) {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(Text.literal("Specify new text..."));
        inputGui.setDefaultInputValue("New Faction Description");

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    String desc = inputGui.getInput();
                    faction.setDescription(desc);
                    new Message("Successfully updated faction description to '" + desc + "'")
                            .prependFaction(faction).send(player, false);
                    this.open();
                }
        );

        inputGui.open();
    }

    private void execMOTD(Faction faction) {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(Text.literal("Specify a MOTD..."));
        inputGui.setDefaultInputValue("New Faction MOTD");

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    String motd = inputGui.getInput();
                    faction.setMOTD(motd);
                    new Message("Successfully updated faction MOTD to '" + motd + "'")
                            .prependFaction(faction).send(player, false);
                    this.open();
                }
        );

        inputGui.open();
    }
}

class ColorGui extends SimpleGui {
    ServerPlayerEntity player;
    Faction faction;
    Runnable returnCallback;

    public ColorGui(ServerPlayerEntity player, Faction faction, @Nullable Runnable returnCallback) {
        super(ScreenHandlerType.GENERIC_9X2, player, false);
        this.player = player;
        this.faction = faction;
        this.returnCallback = returnCallback;

        this.setTitle(Text.literal("Select a color"));

        this.addSlot(new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Text.literal("Go back").formatted(Formatting.RED))
                .setCallback(returnCallback)
        );
        this.addSlot(new GuiElementBuilder(Items.BARRIER)
                .setName(Text.literal("Reset color"))
                .setCallback(() -> colorCallback(Formatting.RESET))
        );
        this.addSlot(new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());
        this.addSlot(new GuiElementBuilder(Items.RED_CONCRETE_POWDER)
                .setName(Text.literal("Use red color").formatted(Formatting.RED))
                .setCallback(() -> colorCallback(Formatting.RED))
        );
        this.addSlot(new GuiElementBuilder(Items.LIGHT_BLUE_CONCRETE)
                .setName(Text.literal("Use aqua color").formatted(Formatting.AQUA))
                .setCallback(() -> colorCallback(Formatting.AQUA))
        );
        this.addSlot(new GuiElementBuilder(Items.BLACK_CONCRETE)
                .setName(Text.literal("Use black color").formatted(Formatting.BLACK))
                .setCallback(() -> colorCallback(Formatting.BLACK))
        );
        this.addSlot(new GuiElementBuilder(Items.BLUE_CONCRETE_POWDER)
                .setName(Text.literal("Use blue color").formatted(Formatting.BLUE))
                .setCallback(() -> colorCallback(Formatting.BLUE))
        );
        this.addSlot(new GuiElementBuilder(Items.CYAN_CONCRETE)
                .setName(Text.literal("Use dark_aqua color").formatted(Formatting.DARK_AQUA))
                .setCallback(() -> colorCallback(Formatting.DARK_AQUA))
        );
        this.addSlot(new GuiElementBuilder(Items.BLUE_CONCRETE)
                .setName(Text.literal("Use dark_blue color").formatted(Formatting.DARK_BLUE))
                .setCallback(() -> colorCallback(Formatting.DARK_BLUE))
        );
        this.addSlot(new GuiElementBuilder(Items.GRAY_CONCRETE)
                .setName(Text.literal("Use dark_gray color").formatted(Formatting.DARK_GRAY))
                .setCallback(() -> colorCallback(Formatting.DARK_GRAY))
        );
        this.addSlot(new GuiElementBuilder(Items.LIGHT_GRAY_CONCRETE)
                .setName(Text.literal("Use gray color").formatted(Formatting.GRAY))
                .setCallback(() -> colorCallback(Formatting.GRAY))
        );
        this.addSlot(new GuiElementBuilder(Items.GREEN_CONCRETE)
                .setName(Text.literal("Use dark_green color").formatted(Formatting.DARK_GREEN))
                .setCallback(() -> colorCallback(Formatting.DARK_GREEN))
        );
        this.addSlot(new GuiElementBuilder(Items.LIME_CONCRETE)
                .setName(Text.literal("Use green color").formatted(Formatting.GREEN))
                .setCallback(() -> colorCallback(Formatting.GREEN))
        );
        this.addSlot(new GuiElementBuilder(Items.MAGENTA_CONCRETE)
                .setName(Text.literal("Use dark_purple color").formatted(Formatting.DARK_PURPLE))
                .setCallback(() -> colorCallback(Formatting.DARK_PURPLE))
        );
        this.addSlot(new GuiElementBuilder(Items.PINK_CONCRETE)
                .setName(Text.literal("Use light_purple color").formatted(Formatting.LIGHT_PURPLE))
                .setCallback(() -> colorCallback(Formatting.LIGHT_PURPLE))
        );
        this.addSlot(new GuiElementBuilder(Items.RED_CONCRETE)
                .setName(Text.literal("Use dark_red color").formatted(Formatting.DARK_RED))
                .setCallback(() -> colorCallback(Formatting.DARK_RED))
        );
        this.addSlot(new GuiElementBuilder(Items.YELLOW_CONCRETE)
                .setName(Text.literal("Use gold color").formatted(Formatting.GOLD))
                .setCallback(() -> colorCallback(Formatting.GOLD))
        );
        this.addSlot(new GuiElementBuilder(Items.WHITE_CONCRETE)
                .setName(Text.literal("Use white color").formatted(Formatting.WHITE))
                .setCallback(() -> colorCallback(Formatting.WHITE))
        );
        this.open();
    }

    private void colorCallback(Formatting color) {
        faction.setColor(color);
        returnCallback.run();
        new Message(
                "Successfully updated faction color to " + Formatting.BOLD + color + color.name())
                .prependFaction(faction).send(player, false);
        return;
    }
}