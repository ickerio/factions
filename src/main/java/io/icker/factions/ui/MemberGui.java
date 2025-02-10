package io.icker.factions.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.command.RankCommand;
import io.icker.factions.util.Command;
import io.icker.factions.util.GuiInteract;
import io.icker.factions.util.Icons;
import io.icker.factions.util.Message;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MemberGui extends PagedGui {
    Faction faction;
    int size;
    UserCache cache;
    User user;

    List<User> members;

    public MemberGui(ServerPlayerEntity player, Faction faction, @Nullable Runnable closeCallback) {
        super(player, closeCallback);
        this.faction = faction;
        this.cache = player.getServer().getUserCache();
        this.user = User.get(player.getUuid());

        this.members = new ArrayList<>(faction.getUsers());
        this.members.remove(user);
        this.members.addFirst(user);
        this.size = members.size();

        this.setTitle(
                Text.literal(faction.getColor() + faction.getName())
                        .append(Text.literal(" members list (" + size + ")")));
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
            var targetUser = this.members.get(id);
            GameProfile unknownPlayer = new GameProfile(UUID.randomUUID(), "{Unknown Player}");
            unknownPlayer
                    .getProperties()
                    .put("textures", new Property("textures", Icons.GUI_UNKNOWN_PLAYER, null));

            GameProfile profile = cache.getByUuid(targetUser.getID()).orElse(unknownPlayer);

            var icon = new GuiElementBuilder(Items.PLAYER_HEAD);
            icon.setComponent(DataComponentTypes.PROFILE, new ProfileComponent(profile));
            icon.setName(Text.literal(profile.getName()));

            if (profile == unknownPlayer) {
                List<Text> lore =
                        List.of(
                                Text.literal("No info available")
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(Formatting.GRAY)));
                icon.setLore(lore);
                return DisplayElement.of(icon);
            }

            List<Text> lore =
                    new ArrayList<>(
                            List.of(
                                    Text.literal("Rank: ")
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withItalic(false)
                                                            .withColor(Formatting.GRAY))
                                            .append(
                                                    Text.literal(targetUser.getRankName())
                                                            .setStyle(
                                                                    Style.EMPTY
                                                                            .withItalic(false)
                                                                            .withColor(
                                                                                    Formatting
                                                                                            .GREEN)))));
            if (!profile.getId().equals(player.getUuid())
                    && Command.Requires.isLeader().test(player.getCommandSource())
                    && Command.Requires.hasPerms("factions.rank.promote", 0)
                            .test(player.getCommandSource())) {
                lore.add(
                        Text.literal("Click to promote.")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(Formatting.DARK_GREEN)));
                lore.add(
                        Text.literal("Right-click to demote.")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(Formatting.DARK_RED)));
                lore.add(
                        Text.literal("Drop (Q) to kick.")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(Formatting.DARK_RED)));
                ServerPlayerEntity targetPlayer =
                        player.getServer().getPlayerManager().getPlayer(targetUser.getID());
                icon.setCallback(
                        (index, clickType, actionType) -> {
                            GuiInteract.playClickSound(player);
                            if (clickType == ClickType.MOUSE_LEFT) {
                                try {
                                    RankCommand.execPromote(targetUser, player);
                                    new Message(
                                                    "Promoted "
                                                            + profile.getName()
                                                            + " to "
                                                            + User.get(profile.getId())
                                                                    .getRankName())
                                            .prependFaction(faction)
                                            .send(player, false);
                                } catch (Exception e) {
                                    new Message(e.getMessage())
                                            .format(Formatting.RED)
                                            .send(player, false);
                                    return;
                                }
                            }
                            if (clickType == ClickType.MOUSE_RIGHT) {
                                try {
                                    RankCommand.execDemote(targetUser, player);

                                    new Message(
                                                    "Demoted "
                                                            + profile.getName()
                                                            + " to "
                                                            + User.get(profile.getId())
                                                                    .getRankName())
                                            .prependFaction(faction)
                                            .send(player, false);
                                } catch (Exception e) {
                                    new Message(e.getMessage())
                                            .format(Formatting.RED)
                                            .send(player, false);
                                    return;
                                }
                            }
                            if (clickType == ClickType.DROP) {
                                SimpleGui gui =
                                        new SimpleGui(ScreenHandlerType.HOPPER, player, false);
                                for (int i = 0; i < 5; i++)
                                    gui.setSlot(
                                            i,
                                            new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                                                    .hideTooltip());
                                gui.setTitle(Text.literal("Are you sure?"));
                                gui.setSlot(
                                        1,
                                        new GuiElementBuilder(Items.SLIME_BALL)
                                                .setName(
                                                        Text.literal("Click to confirm")
                                                                .formatted(Formatting.GREEN))
                                                .setCallback(
                                                        ((index2, clickType2, actionType2) -> {
                                                            GuiInteract.playClickSound(player);
                                                            targetUser.leaveFaction();
                                                            new Message(
                                                                            "Kicked "
                                                                                    + player.getName()
                                                                                            .getString())
                                                                    .send(player, false);

                                                            if (targetPlayer != null) {
                                                                new Message(
                                                                                "You have been"
                                                                                    + " kicked from"
                                                                                    + " the faction"
                                                                                    + " by "
                                                                                        + player.getName()
                                                                                                .getString())
                                                                        .send(targetPlayer, false);
                                                            }
                                                            this.open();
                                                        })));
                                gui.setSlot(
                                        3,
                                        new GuiElementBuilder(Items.STRUCTURE_VOID)
                                                .setName(
                                                        Text.literal("Click to go back")
                                                                .formatted(Formatting.RED))
                                                .setCallback(
                                                        () -> {
                                                            GuiInteract.playClickSound(player);
                                                            this.open();
                                                        }));
                                gui.open();
                            }
                            lore.removeFirst();
                            lore.addFirst(
                                    Text.literal("Rank: ")
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withItalic(false)
                                                            .withColor(Formatting.GRAY))
                                            .append(
                                                    Text.literal(targetUser.getRankName())
                                                            .setStyle(
                                                                    Style.EMPTY
                                                                            .withItalic(false)
                                                                            .withColor(
                                                                                    Formatting
                                                                                            .GREEN))));
                            icon.setLore(lore);
                        });
            }
            icon.setLore(lore);

            return DisplayElement.of(icon);
        }
        return DisplayElement.empty();
    }
}
