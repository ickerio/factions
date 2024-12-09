package io.icker.factions.ui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.api.persistents.User;
import io.icker.factions.command.HomeCommand;
import io.icker.factions.util.Icons;
import io.icker.factions.util.WorldUtils;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class ListGui extends PagedGui {
    List<Faction> factions;
    int size;
    User user;

    public ListGui(ServerPlayerEntity player, User user) {
        super(player, null);
        this.user = user;

        Faction userFaction = user.getFaction();
        this.factions = new ArrayList<>(Faction.all().stream().toList());
        this.factions.remove(userFaction);
        this.factions.addFirst(userFaction);
        this.size = factions.size();

        this.setTitle(Text.literal("Factions list"));
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
            icon.setSkullOwner(isInFaction ? Icons.GUI_FACTION_MEMBER : Icons.GUI_FACTION_GUEST);
            icon.setName(Text.literal(faction.getName()));

            List<Text> lore = new ArrayList<>(List.of(Text.literal(faction.getDescription()).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))));
            if (isInFaction && home != null) {
                lore.add(Text.literal("Click to view faction info.").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
                lore.add(Text.literal("Right-click to teleport to faction home.").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.DARK_AQUA)));
                icon.setCallback((index, clickType, actionType) -> {
                    if (clickType == ClickType.MOUSE_RIGHT) {
                        new HomeCommand().execGo(player, faction);
                        this.close();
                        return;
                    }
                    InfoGui infoGui = new InfoGui(player, faction, this::open);
                });
            } else {
                lore.add(Text.literal("Click to view faction info.").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
                icon.setCallback((index, clickType, actionType) -> {
                    InfoGui infoGui = new InfoGui(player, faction, this::open);
                });
            }
            icon.setLore(lore);

            return DisplayElement.of(icon);
        }

        return DisplayElement.empty();
    }
}
