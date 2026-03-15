package io.icker.factions.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.command.ModifyCommand;
import io.icker.factions.util.GuiInteract;
import io.icker.factions.util.Icons;
import io.icker.factions.util.Message;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import xyz.nucleoid.server.translations.api.Localization;

public class ModifyGui extends SimpleGui {
    Runnable closeCallback;
    private final Runnable defaultReturn =
            () -> {
                GuiInteract.playClickSound(player);
                this.open();
            };

    public ModifyGui(ServerPlayer player, Faction faction, @Nullable Runnable closeCallback) {
        super(MenuType.GENERIC_9x1, player, false);
        this.closeCallback = closeCallback;

        this.setTitle(Component.translatable("factions.gui.modify.title"));
        for (int i = 0; i < 9; i++)
            this.setSlot(i, new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());

        this.setSlot(
                0,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_TV_TEXT)
                        .setName(Component.translatable("factions.gui.modify.change_name"))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    this.execName(faction);
                                }));
        this.setSlot(
                1,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_BOOK)
                        .setName(Component.translatable("factions.gui.modify.change_description"))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    this.execDesc(faction);
                                }));
        this.setSlot(
                2,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_RADIO)
                        .setName(Component.translatable("factions.gui.modify.change_motd"))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    this.execMOTD(faction);
                                }));
        this.setSlot(
                4,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_PAINT_BUCKET)
                        .setName(Component.translatable("factions.gui.modify.change_color"))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    new ColorGui(player, faction, this::open);
                                }));
        this.setSlot(5, buildOpenFactionButton(faction));

        this.setSlot(
                8,
                new GuiElementBuilder(Items.STRUCTURE_VOID)
                        .setName(
                                closeCallback == null
                                        ? Component.translatable("factions.gui.generic.close")
                                                .withStyle(ChatFormatting.RED)
                                        : Component.translatable("factions.gui.generic.back")
                                                .withStyle(ChatFormatting.RED))
                        .setCallback(
                                closeCallback == null
                                        ? (Runnable) this::close
                                        : (Runnable) closeCallback));

        this.open();
    }

    private GuiElementBuilder buildOpenFactionButton(Faction faction) {
        return new GuiElementBuilder(Items.PLAYER_HEAD)
                .setProfileSkinTexture(
                        faction.isOpen() ? Icons.GUI_TESSERACT_BLUE : Icons.GUI_TESSERACT_RED)
                .setName(
                        Component.translatable(
                                "factions.gui.modify.faction_type",
                                faction.isOpen()
                                        ? Component.translatable(
                                                        "factions.gui.modify.faction_type.public")
                                                .withStyle(ChatFormatting.AQUA)
                                        : Component.translatable(
                                                        "factions.gui.modify.faction_type.invite")
                                                .withStyle(ChatFormatting.RED)))
                .setCallback(
                        (index, clickType, actionType) -> {
                            faction.setOpen(!faction.isOpen());
                            this.setSlot(index, buildOpenFactionButton(faction));
                        });
    }

    private void execName(Faction faction) {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(Component.translatable("factions.gui.modify.change_name.input.title"));
        inputGui.setDefaultInputValue(
                Localization.raw("factions.gui.modify.change_name.input.default", player));

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    String name = inputGui.getInput();

                    try {
                        ModifyCommand.execName(player, faction, name);
                        new Message(
                                        Component.translatable(
                                                "factions.gui.modify.change_name.result", name))
                                .prependFaction(faction)
                                .send(player, false);
                    } catch (Exception e) {
                        inputGui.showErrorMessage(e.getMessage(), index);
                        return;
                    }

                    this.open();
                });

        inputGui.open();
    }

    private void execDesc(Faction faction) {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(
                Component.translatable("factions.gui.modify.change_description.input.title"));
        inputGui.setDefaultInputValue(
                Localization.raw("factions.gui.modify.change_description.input.default", player));

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    String desc = inputGui.getInput();
                    faction.setDescription(desc);
                    new Message(
                                    Component.translatable(
                                            "factions.gui.modify.change_description.result", desc))
                            .prependFaction(faction)
                            .send(player, false);
                    this.open();
                });

        inputGui.open();
    }

    private void execMOTD(Faction faction) {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(Component.translatable("factions.gui.modify.change_motd.input.title"));
        inputGui.setDefaultInputValue(
                Localization.raw("factions.gui.modify.change_motd.input.default", player));

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    String motd = inputGui.getInput();
                    faction.setMOTD(motd);
                    new Message(
                                    Component.translatable(
                                            "factions.gui.modify.change_motd.result", motd))
                            .prependFaction(faction)
                            .send(player, false);
                    this.open();
                });

        inputGui.open();
    }
}

class ColorGui extends SimpleGui {
    ServerPlayer player;
    Faction faction;
    Runnable returnCallback;

    public ColorGui(ServerPlayer player, Faction faction, @Nullable Runnable returnCallback) {
        super(MenuType.GENERIC_9x2, player, false);
        this.player = player;
        this.faction = faction;
        this.returnCallback = returnCallback;

        this.setTitle(Component.translatable("factions.gui.modify.change_color.select.title"));

        this.addSlot(
                new GuiElementBuilder(Items.STRUCTURE_VOID)
                        .setName(
                                Component.translatable("factions.gui.generic.back")
                                        .withStyle(ChatFormatting.RED))
                        .setCallback(returnCallback));
        this.addSlot(
                new GuiElementBuilder(Items.BARRIER)
                        .setName(
                                Component.translatable(
                                        "factions.gui.modify.change_color.select.option.reset"))
                        .setCallback(() -> colorCallback(ChatFormatting.RESET)));
        this.addSlot(new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());
        this.addSlot(
                new GuiElementBuilder(Items.RED_CONCRETE_POWDER)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.red")
                                        .withStyle(ChatFormatting.RED))
                        .setCallback(() -> colorCallback(ChatFormatting.RED)));
        this.addSlot(
                new GuiElementBuilder(Items.LIGHT_BLUE_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.aqua")
                                        .withStyle(ChatFormatting.AQUA))
                        .setCallback(() -> colorCallback(ChatFormatting.AQUA)));
        this.addSlot(
                new GuiElementBuilder(Items.BLACK_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.black")
                                        .withStyle(ChatFormatting.BLACK))
                        .setCallback(() -> colorCallback(ChatFormatting.BLACK)));
        this.addSlot(
                new GuiElementBuilder(Items.BLUE_CONCRETE_POWDER)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.blue")
                                        .withStyle(ChatFormatting.BLUE))
                        .setCallback(() -> colorCallback(ChatFormatting.BLUE)));
        this.addSlot(
                new GuiElementBuilder(Items.CYAN_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.dark_aqua")
                                        .withStyle(ChatFormatting.DARK_AQUA))
                        .setCallback(() -> colorCallback(ChatFormatting.DARK_AQUA)));
        this.addSlot(
                new GuiElementBuilder(Items.BLUE_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.dark_blue")
                                        .withStyle(ChatFormatting.DARK_BLUE))
                        .setCallback(() -> colorCallback(ChatFormatting.DARK_BLUE)));
        this.addSlot(
                new GuiElementBuilder(Items.GRAY_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.dark_gray")
                                        .withStyle(ChatFormatting.DARK_GRAY))
                        .setCallback(() -> colorCallback(ChatFormatting.DARK_GRAY)));
        this.addSlot(
                new GuiElementBuilder(Items.LIGHT_GRAY_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.gray")
                                        .withStyle(ChatFormatting.GRAY))
                        .setCallback(() -> colorCallback(ChatFormatting.GRAY)));
        this.addSlot(
                new GuiElementBuilder(Items.GREEN_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.dark_green")
                                        .withStyle(ChatFormatting.DARK_GREEN))
                        .setCallback(() -> colorCallback(ChatFormatting.DARK_GREEN)));
        this.addSlot(
                new GuiElementBuilder(Items.LIME_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.green")
                                        .withStyle(ChatFormatting.GREEN))
                        .setCallback(() -> colorCallback(ChatFormatting.GREEN)));
        this.addSlot(
                new GuiElementBuilder(Items.MAGENTA_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.dark_purple")
                                        .withStyle(ChatFormatting.DARK_PURPLE))
                        .setCallback(() -> colorCallback(ChatFormatting.DARK_PURPLE)));
        this.addSlot(
                new GuiElementBuilder(Items.PINK_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.light_purple")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                        .setCallback(() -> colorCallback(ChatFormatting.LIGHT_PURPLE)));
        this.addSlot(
                new GuiElementBuilder(Items.RED_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.dark_red")
                                        .withStyle(ChatFormatting.DARK_RED))
                        .setCallback(() -> colorCallback(ChatFormatting.DARK_RED)));
        this.addSlot(
                new GuiElementBuilder(Items.YELLOW_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.gold")
                                        .withStyle(ChatFormatting.GOLD))
                        .setCallback(() -> colorCallback(ChatFormatting.GOLD)));
        this.addSlot(
                new GuiElementBuilder(Items.WHITE_CONCRETE)
                        .setName(
                                Component.translatable(
                                                "factions.gui.modify.change_color.select.option.white")
                                        .withStyle(ChatFormatting.WHITE))
                        .setCallback(() -> colorCallback(ChatFormatting.WHITE)));
        this.open();
    }

    private void colorCallback(ChatFormatting color) {
        faction.setColor(color);
        returnCallback.run();
        if (color.equals(ChatFormatting.RESET)) {
            new Message(Component.translatable("factions.gui.modify.change_color.result.reset"))
                    .prependFaction(faction)
                    .send(player, false);
        } else {
            new Message(
                            Component.translatable(
                                    "factions.gui.modify.change_color.result.color",
                                    Component.translatable(
                                                    "factions.gui.modify.change_color.color."
                                                            + color.name().toLowerCase())
                                            .setStyle(Style.EMPTY.withColor(color).withBold(true))))
                    .prependFaction(faction)
                    .send(player, false);
        }
        return;
    }
}
