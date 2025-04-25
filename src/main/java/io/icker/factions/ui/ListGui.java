package io.icker.factions.ui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.api.persistents.User;
import io.icker.factions.command.HomeCommand;
import io.icker.factions.util.GuiInteract;
import io.icker.factions.util.Icons;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ListGui extends PagedGui {
    List<Faction> factions;
    int size;
    User user;

    public ListGui(ServerPlayerEntity player, User user, @Nullable Runnable closeCallback) {
        super(player, closeCallback);
        this.user = user;

        this.factions = new ArrayList<>(Faction.all().stream().toList());
        Faction userFaction;
        if ((userFaction = user.getFaction()) != null) {
            this.factions.remove(userFaction);
            this.factions.addFirst(userFaction);
        }
        this.size = factions.size();

        this.setTitle(Text.translatable("factions.gui.list.title"));
        this.updateDisplay();
        this.open();
    }

    @Override
    protected int getPageAmount() {
        return this.size / PAGE_SIZE;
    }

    @Override
    protected DisplayElement getElement(int id) {
        if (this.size > id) {
            var faction = this.factions.get(id);

            boolean isInFaction = this.user.getFaction() == faction;
            Home home = faction.getHome();

            var icon = new GuiElementBuilder(Items.PLAYER_HEAD);
            icon.setSkullOwner(isInFaction ? Icons.GUI_CASTLE_NORMAL : Icons.GUI_CASTLE_OPEN);
            icon.setName(Text.literal(faction.getColor() + faction.getName()));

            List<Text> lore = new ArrayList<>(List.of(Text.literal(faction.getDescription())
                    .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))));
            if (isInFaction && home != null) {
                lore.add(Text.translatable("factions.gui.list.entry.view_info")
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
                lore.add(Text.translatable("factions.gui.list.entry.teleport")
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.DARK_AQUA)));
                icon.setCallback((index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    if (clickType == ClickType.MOUSE_RIGHT) {
                        new HomeCommand().execGo(player, faction);
                        this.close();
                        return;
                    }
                    new InfoGui(player, faction, this::open);
                });
            } else {
                lore.add(Text.translatable("factions.gui.list.entry.view_info")
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
                icon.setCallback((index, clickType, actionType) -> {
                    GuiInteract.playClickSound(player);
                    new InfoGui(player, faction, this::open);
                });
            }
            icon.setLore(lore);

            return DisplayElement.of(icon);
        }

        return DisplayElement.empty();
    }
}
