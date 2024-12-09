package io.icker.factions.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Icons;
import io.icker.factions.util.Message;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ModifyGui extends SimpleGui {
    /**
     * Constructs a new simple container gui for the supplied player.
     *
     * @param player                the player to server this gui to
     *                              will be treated as slots of this gui
     */
    public ModifyGui(ServerPlayerEntity player, Faction faction) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);

        this.setTitle(Text.literal("Faction settings"));

        this.setSlot(0, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_TV_TEXT)
                .setName(Text.literal("Change name"))
                .setCallback((index, clickType, actionType) -> {
                    AnvilInputGui inputGui = new AnvilInputGui(player, false);
                    inputGui.setTitle(Text.literal("Specify a name..."));
                    inputGui.setDefaultInputValue(faction.getName());
                    inputGui.setSlot(1, new GuiElementBuilder(Items.BARRIER)
                            .setName(Text.literal("Cancel"))
                            .setCallback(this::open)
                    );
                    inputGui.setSlot(2, new GuiElementBuilder(Items.SLIME_BALL)
                            .setName(Text.literal("Confirm"))
                            .setCallback(() -> {
                                String name = inputGui.getInput();
                                faction.setName(name);
                                new Message("Successfully renamed faction to '" + name + "'").prependFaction(faction)
                                        .send(player, false);
                                this.open();
                            })
                    );
                    inputGui.open();
                })
        );
        this.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_BOOK)
                .setName(Text.literal("Change description"))
                .setCallback((index, clickType, actionType) -> {
                    AnvilInputGui inputGui = new AnvilInputGui(player, false);
                    inputGui.setTitle(Text.literal("Specify a description..."));
                    inputGui.setDefaultInputValue(faction.getDescription());
                    inputGui.setSlot(1, new GuiElementBuilder(Items.BARRIER)
                            .setName(Text.literal("Cancel"))
                            .setCallback(this::open)
                    );
                    inputGui.setSlot(2, new GuiElementBuilder(Items.SLIME_BALL)
                            .setName(Text.literal("Confirm"))
                            .setCallback(() -> {
                                String description = inputGui.getInput();
                                faction.setDescription(description);
                                new Message("Successfully updated faction description to '" + description + "'")
                                        .prependFaction(faction).send(player, false);
                                this.open();
                            })
                    );
                    inputGui.open();
                })
        );
        this.open();
    }
}
