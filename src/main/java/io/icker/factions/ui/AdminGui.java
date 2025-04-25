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
import xyz.nucleoid.server.translations.api.Localization;

import java.util.*;

public class AdminGui extends SimpleGui {
    private final Runnable defaultReturn = () -> {
        GuiInteract.playClickSound(player);
        this.open();
    };

    public AdminGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        User user = User.get(player.getUuid());

        // GUI
        this.setTitle(Text.translatable("factions.gui.admin.title"));

        List<Integer> indexes = FactionsMod.dynmap == null ? List.of(1, 3, 5, 7) : List.of(0, 2, 4, 6, 8);

        for (int i = 0; i < 9; i++)
            this.setSlot(i, new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());

        // Bypass icon
        this.setSlot(indexes.get(0), new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(user.bypass ? Icons.GUI_TESSERACT_BLUE : Icons.GUI_TESSERACT_OFF)
                .setName(Text.translatable("factions.gui.admin.options.bypass")
                        .append(user.bypass ? Text.translatable("options.on").formatted(Formatting.GREEN)
                                : Text.translatable("options.off").formatted(Formatting.RED)))
                .setLore(
                        List.of(
                                Text.translatable(
                                        user.bypass
                                                ? "factions.gui.admin.options.bypass.lore.disable"
                                                : "factions.gui.admin.options.bypass.lore.enable")
                                        .setStyle(
                                                Style.EMPTY.withItalic(false)
                                                        .withColor(Formatting.GRAY))))
                .setCallback((index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    user.bypass = !user.bypass;
                    ItemStack item = this.getSlot(index).getItemStack();
                    item.set(DataComponentTypes.ITEM_NAME, Text.translatable("factions.gui.admin.options.bypass")
                            .append(user.bypass ? Text.translatable("options.on").formatted(Formatting.GREEN)
                                    : Text.translatable("options.off").formatted(Formatting.RED)));
                    item.set(DataComponentTypes.LORE,
                            new LoreComponent(
                                    List.of(
                                            Text.translatable(
                                                    user.bypass
                                                            ? "factions.gui.admin.options.bypass.lore.disable"
                                                            : "factions.gui.admin.options.bypass.lore.enable")
                                                    .setStyle(
                                                            Style.EMPTY.withItalic(false)
                                                                    .withColor(Formatting.GRAY)))));

                    PropertyMap map = new PropertyMap();
                    map.put("textures", new Property("textures",
                            user.bypass ? Icons.GUI_TESSERACT_BLUE : Icons.GUI_TESSERACT_OFF, null));
                    item.set(DataComponentTypes.PROFILE, new ProfileComponent(Optional.empty(), Optional.empty(), map));

                    new Message(Text.translatable("factions.gui.admin.options.bypass.success")).filler("·")
                            .add(new Message(
                                    user.bypass ? Text.translatable("options.on") : Text.translatable("options.off"))
                                    .format(user.bypass ? Formatting.GREEN : Formatting.RED))
                            .send(player, false);
                }));

        // Power icon
        this.setSlot(indexes.get(1), new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_FIST)
                .setName(Text.translatable("factions.gui.admin.options.power"))
                .setLore(List.of(Text.translatable("factions.gui.admin.options.power.lore")
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))))
                .setCallback((index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    this.execPower();
                }));

        // Spoof icon
        this.setSlot(indexes.get(2), new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_XENOMORPH)
                .setName(Text.translatable("factions.gui.admin.options.spoof"))
                .setLore(List.of(
                        Text.translatable("factions.gui.admin.options.spoof.lore1")
                                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)),
                        Text.translatable("factions.gui.admin.options.spoof.lore2")
                                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))))
                .setCallback((index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    if (clickType == ClickType.MOUSE_RIGHT) {
                        user.setSpoof(null);
                        new Message(Text.translatable("factions.gui.admin.options.spoof.clear.success")).send(player,
                                false);
                        return;
                    }
                    this.execSpoof();
                }));

        // Audit icon
        this.setSlot(
                indexes.get(3),
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setSkullOwner(Icons.GUI_BOOK)
                        .setName(Text.translatable("factions.gui.admin.options.audit"))
                        .setLore(
                                List.of(
                                        Text.translatable("factions.gui.admin.options.audit.lore")
                                                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))))
                        .setCallback((index, clickType, actionType) -> {
                            GuiInteract.playClickSound(player);
                            for (int i = 0; i < 4; i++) {
                                Claim.audit();
                                Faction.audit();
                                User.audit();
                            }

                            new Message(Text.translatable("factions.gui.admin.options.audit.success")).send(player,
                                    false);
                        }));

        if (indexes.size() > 4) {
            // Dynmap reload icon
            this.setSlot(
                    indexes.get(4),
                    new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setSkullOwner(Icons.GUI_EARTH_RELOAD)
                            .setName(Text.translatable("factions.gui.admin.options.reload_dynmap"))
                            .setLore(
                                    List.of(
                                            Text.translatable("factions.gui.admin.options.reload_dynmap.lore")
                                                    .setStyle(
                                                            Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))))
                            .setCallback((index, clickType, actionType) -> {
                                GuiInteract.playClickSound(player);
                                FactionsMod.dynmap.reloadAll();
                                new Message(Text.translatable("factions.gui.admin.options.reload_dynmap.success")).send(
                                        player,
                                        false);
                            }));
        }

        this.open();
    }

    private void execSpoof() {
        InputGui inputGui = new InputGui(player);

        inputGui.setTitle(Text.translatable("factions.gui.spoof.title"));
        inputGui.setDefaultInputValue(Localization.raw("factions.gui.spoof.default", player));

        inputGui.returnBtn.setCallback(defaultReturn);
        inputGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);

                    String input = inputGui.getInput();

                    if (!input.matches("^[a-zA-Z0-9_]{2,16}$")) {
                        inputGui.showErrorMessage(
                                Text.translatable("factions.gui.spoof.error.invalid_name", input)
                                        .formatted(Formatting.RED),
                                index);
                        return;
                    }
                    Optional<GameProfile> profile;
                    if (!(profile = player.getServer().getUserCache().findByName(input)).isPresent()) {
                        inputGui.showErrorMessage(
                                Text.translatable("factions.gui.spoof.error.no_player", input)
                                        .formatted(Formatting.RED),
                                index);
                        return;
                    }
                    User target = User.get(profile.get().getId());
                    User.get(player.getUuid()).setSpoof(target);
                    new Message(Text.translatable("factions.gui.spoof.success", input)).send(player, false);
                    this.open();
                });

        inputGui.open();
    }

    private void execPower() {
        InputGui inputFacGui = new InputGui(player);
        InputGui inputPowGui = new InputGui(player);

        final Faction[] selectedFac = new Faction[1];
        final int[] selectedPow = new int[1];

        inputFacGui.setTitle(Text.translatable("factions.gui.power.setfaction.title"));
        inputFacGui.setDefaultInputValue(Localization.raw("factions.gui.power.setfaction.default", player));

        inputFacGui.returnBtn.setCallback(defaultReturn);
        inputFacGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    selectedFac[0] = Faction.getByName(inputFacGui.getInput());
                    if (selectedFac[0] == null) {
                        inputFacGui.showErrorMessage(
                                Text.translatable("factions.gui.power.setfaction.fail.no_faction")
                                        .formatted(Formatting.RED),
                                index);
                        return;
                    }
                    inputPowGui.open();
                });

        inputPowGui.setTitle(Text.translatable("factions.gui.power.setpower.title"));
        inputPowGui.setDefaultInputValue(Localization.raw("factions.gui.power.setpower.default", player));
        inputPowGui.returnBtn.setCallback(defaultReturn);
        inputPowGui.confirmBtn.setCallback(
                (index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    try {
                        selectedPow[0] = Integer.parseInt(inputPowGui.getInput());
                    } catch (Exception e) {
                        inputPowGui.showErrorMessage(
                                Text.translatable("factions.gui.power.setpower.fail.nan").formatted(Formatting.RED),
                                index);
                        return;
                    }

                    selectedFac[0].addAdminPower(selectedPow[0]);

                    if (selectedPow[0] != 0) {
                        if (selectedPow[0] > 0) {
                            new Message(
                                    Text.translatable(
                                            "factions.gui.power.success.added.faction",
                                            player.getName().getString(),
                                            selectedPow[0]))
                                    .send(selectedFac[0]);
                            new Message(Text.translatable("factions.gui.power.success.added.admin", selectedPow[0]))
                                    .send(player, false);
                        } else {
                            new Message(
                                    Text.translatable(
                                            "factions.gui.power.success.removed.faction",
                                            player.getName().getString(),
                                            selectedPow[0]))
                                    .send(selectedFac[0]);
                            new Message(Text.translatable("factions.gui.power.success.removed.admin", selectedPow[0]))
                                    .send(player, false);
                        }
                    } else {
                        new Message(Text.translatable("factions.gui.power.error.nochange")).fail().send(
                                player,
                                false);
                    }
                    this.open();
                });

        inputFacGui.open();
    }
}
