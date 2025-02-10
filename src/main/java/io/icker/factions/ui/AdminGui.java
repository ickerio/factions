package io.icker.factions.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.GuiInteract;
import io.icker.factions.util.Icons;
import io.icker.factions.util.Message;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class AdminGui extends SimpleGui {
    private final Runnable defaultReturn =
            () -> {
                GuiInteract.playClickSound(player);
                this.open();
            };

    public AdminGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        User user = User.get(player.getUuid());

        // GUI
        this.setTitle(Text.literal("Factions admin"));

        List<Integer> indexes =
                FactionsMod.dynmap == null ? List.of(1, 3, 5, 7) : List.of(0, 2, 4, 6, 8);

        for (int i = 0; i < 9; i++)
            this.setSlot(i, new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());

        // Bypass icon
        this.setSlot(
                indexes.get(0),
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(
                                user.bypass ? Icons.GUI_TESSERACT_BLUE : Icons.GUI_TESSERACT_OFF)
                        .setName(
                                Text.literal("Bypass : ")
                                        .append(
                                                user.bypass
                                                        ? Text.translatable("options.on")
                                                                .formatted(Formatting.GREEN)
                                                        : Text.translatable("options.off")
                                                                .formatted(Formatting.RED)))
                        .setLore(
                                List.of(
                                        Text.literal(
                                                        user.bypass
                                                                ? "Click to disable"
                                                                : "Click to enable")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY))))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    GuiInteract.playClickSound(player);
                                    user.bypass = !user.bypass;
                                    ItemStack item = this.getSlot(index).getItemStack();
                                    item.set(
                                            DataComponentTypes.ITEM_NAME,
                                            Text.literal("Bypass : ")
                                                    .append(
                                                            user.bypass
                                                                    ? Text.translatable(
                                                                                    "options.on")
                                                                            .formatted(
                                                                                    Formatting
                                                                                            .GREEN)
                                                                    : Text.translatable(
                                                                                    "options.off")
                                                                            .formatted(
                                                                                    Formatting
                                                                                            .RED)));
                                    item.set(
                                            DataComponentTypes.LORE,
                                            new LoreComponent(
                                                    List.of(
                                                            Text.literal(
                                                                            user.bypass
                                                                                    ? "Click to"
                                                                                          + " disable."
                                                                                    : "Click to"
                                                                                          + " enable.")
                                                                    .setStyle(Style.EMPTY))));

                                    PropertyMap map = new PropertyMap();
                                    map.put(
                                            "textures",
                                            new Property(
                                                    "textures",
                                                    user.bypass
                                                            ? Icons.GUI_TESSERACT_BLUE
                                                            : Icons.GUI_TESSERACT_OFF,
                                                    null));
                                    item.set(
                                            DataComponentTypes.PROFILE,
                                            new ProfileComponent(
                                                    Optional.empty(), Optional.empty(), map));

                                    new Message("Successfully toggled claim bypass")
                                            .filler("·")
                                            .add(
                                                    new Message(user.bypass ? "ON" : "OFF")
                                                            .format(
                                                                    user.bypass
                                                                            ? Formatting.GREEN
                                                                            : Formatting.RED))
                                            .send(player, false);
                                }));

        // Power icon
        this.setSlot(
                indexes.get(1),
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(Icons.GUI_FIST)
                        .setName(Text.literal("Change faction's power"))
                        .setLore(
                                List.of(
                                        Text.literal("Click to change faction's name and power.")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY))))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    GuiInteract.playClickSound(player);
                                    this.execPower();
                                }));

        // Spoof icon
        this.setSlot(
                indexes.get(2),
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(Icons.GUI_XENOMORPH)
                        .setName(Text.literal("Spoof a player"))
                        .setLore(
                                List.of(
                                        Text.literal("Click to specify a person to spoof.")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY)),
                                        Text.literal("Right-click to clear the spoof.")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY))))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    GuiInteract.playClickSound(player);
                                    if (clickType == ClickType.MOUSE_RIGHT) {
                                        user.setSpoof(null);
                                        new Message("Cleared spoof").send(player, false);
                                        return;
                                    }
                                    this.execSpoof();
                                }));

        // Audit icon
        this.setSlot(
                indexes.get(3),
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(Icons.GUI_BOOK)
                        .setName(Text.literal("Run an audit"))
                        .setLore(
                                List.of(
                                        Text.literal(
                                                        "Click to check saved data to ensure"
                                                                + " there's no issues.")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY))))
                        .setCallback(
                                (index, clickType, actionType) -> {
                                    GuiInteract.playClickSound(player);
                                    for (int i = 0; i < 4; i++) {
                                        Claim.audit();
                                        Faction.audit();
                                        User.audit();
                                    }

                                    new Message("Successful audit").send(player, false);
                                }));

        if (indexes.size() > 4) {
            // Dynmap reload icon
            this.setSlot(
                    indexes.get(4),
                    new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setSkullOwner(Icons.GUI_EARTH_RELOAD)
                            .setName(Text.literal("Reload DynMap markers"))
                            .setLore(
                                    List.of(
                                            Text.literal("Reloads dynmap markers.")
                                                    .setStyle(
                                                            Style.EMPTY
                                                                    .withItalic(false)
                                                                    .withColor(Formatting.GRAY))))
                            .setCallback(
                                    (index, clickType, actionType) -> {
                                        GuiInteract.playClickSound(player);
                                        FactionsMod.dynmap.reloadAll();
                                        new Message("Reloaded dynmap marker").send(player, false);
                                    }));
        }

        this.open();
    }

    private void execSpoof() {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(Text.literal("Specify a player..."));
        inputGui.setDefaultInputValue("Player Name");

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);

                    String input = inputGui.getInput();

                    if (!input.matches("^[a-zA-Z0-9_]{2,16}$")) {
                        inputGui.showErrorMessage(
                                Text.literal(String.format("Invalid name %s!", input))
                                        .formatted(Formatting.RED),
                                index);
                        return;
                    }
                    Optional<GameProfile> profile;
                    if (!(profile = player.getServer().getUserCache().findByName(input))
                            .isPresent()) {
                        inputGui.showErrorMessage(
                                Text.literal(String.format("No such player %s!", input))
                                        .formatted(Formatting.RED),
                                index);
                        return;
                    }
                    User target = User.get(profile.get().getId());
                    User.get(player.getUuid()).setSpoof(target);
                    new Message("Set spoof to player %s", input).send(player, false);
                    this.open();
                });

        inputGui.open();
    }

    private void execPower() {
        InputGui inputFacGui = new InputGui(player);
        InputGui inputPowGui = new InputGui(player);

        final Faction[] selectedFac = new Faction[1];
        final int[] selectedPow = new int[1];

        inputFacGui.setTitle(Text.literal("Specify a faction..."));
        inputFacGui.setDefaultInputValue("Faction Name");

        inputFacGui.returnBtn.setCallback(defaultReturn);
        inputFacGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    selectedFac[0] = Faction.getByName(inputFacGui.getInput());
                    if (selectedFac[0] == null) {
                        inputFacGui.showErrorMessage(
                                Text.literal("No such faction!").formatted(Formatting.RED), index);
                        return;
                    }
                    inputPowGui.open();
                });

        inputPowGui.setTitle(Text.literal("Specify power..."));
        inputPowGui.setDefaultInputValue("Power Level");

        inputPowGui.returnBtn.setCallback(defaultReturn);
        inputPowGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    try {
                        selectedPow[0] = Integer.parseInt(inputPowGui.getInput());
                    } catch (Exception e) {
                        inputPowGui.showErrorMessage(
                                Text.literal("Not a number!").formatted(Formatting.RED), index);
                        return;
                    }

                    selectedFac[0].addAdminPower(selectedPow[0]);

                    if (selectedPow[0] != 0) {
                        if (selectedPow[0] > 0) {
                            new Message(
                                            "Admin %s added %d power",
                                            player.getName().getString(), selectedPow[0])
                                    .send(selectedFac[0]);
                            new Message("Added %d power", selectedPow[0]).send(player, false);
                        } else {
                            new Message(
                                            "Admin %s removed %d power",
                                            player.getName().getString(), selectedPow[0])
                                    .send(selectedFac[0]);
                            new Message("Removed %d power", selectedPow[0]).send(player, false);
                        }
                    } else {
                        new Message("No change to power").fail().send(player, false);
                    }
                    this.open();
                });

        inputFacGui.open();
    }
}
