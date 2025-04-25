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

import xyz.nucleoid.server.translations.api.Localization;

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
        if (faction.equals(this.user.getFaction())) {
            this.members.remove(user);
            this.members.addFirst(user);
        }
        this.size = members.size();

        this.setTitle(
                Text.translatable(
                        "factions.gui.members.title",
                        Text.literal(faction.getColor() + faction.getName()),
                        size));
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
            GameProfile unknownPlayer =
                    new GameProfile(
                            UUID.randomUUID(),
                            Localization.raw("factions.gui.generic.unknown_player", player));
            unknownPlayer
                    .getProperties()
                    .put("textures", new Property("textures", Icons.GUI_UNKNOWN_PLAYER, null));

            GameProfile profile = cache.getByUuid(targetUser.getID()).orElse(unknownPlayer);

            var icon = new GuiElementBuilder(Items.PLAYER_HEAD);
            icon.setComponent(DataComponentTypes.PROFILE, new ProfileComponent(profile));
            icon.setName(Text.literal(profile.getName()));

            if (profile.equals(unknownPlayer)) {
                List<Text> lore =
                        List.of(
                                Text.translatable("factions.gui.members.entry.unknown_player")
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
                                    Text.translatable(
                                                    "factions.gui.members.entry.info.rank",
                                                    Text.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())
                                                            .setStyle(
                                                                    Style.EMPTY
                                                                            .withItalic(false)
                                                                            .withColor(
                                                                                    Formatting
                                                                                            .GREEN)))
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withItalic(false)
                                                            .withColor(Formatting.GRAY))));

            if (!profile.getId().equals(player.getUuid())
                    && Command.Requires.isLeader().test(player.getCommandSource())
                    && Command.Requires.hasPerms("factions.rank.promote", 0)
                            .test(player.getCommandSource())
                    && faction.equals(user.getFaction())) {
                lore.add(
                        Text.translatable("factions.gui.members.entry.manage.promote")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(Formatting.DARK_GREEN)));
                lore.add(
                        Text.translatable("factions.gui.members.entry.manage.demote")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(Formatting.DARK_RED)));
                lore.add(
                        Text.translatable("factions.gui.members.entry.manage.kick")
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
                                                    Text.translatable(
                                                            "factions.gui.members.entry.manage.promote.result",
                                                            profile.getName(),
                                                            Text.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())))
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
                                                    Text.translatable(
                                                            "factions.gui.members.entry.manage.demote.result",
                                                            profile.getName(),
                                                            Text.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())))
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
                                gui.setTitle(
                                        Text.translatable(
                                                "factions.gui.members.entry.manage.kick.confirm.title"));
                                gui.setSlot(
                                        1,
                                        new GuiElementBuilder(Items.SLIME_BALL)
                                                .setName(
                                                        Text.translatable(
                                                                        "factions.gui.members.entry.manage.kick.confirm.yes",
                                                                        targetPlayer
                                                                                .getName()
                                                                                .getString())
                                                                .formatted(Formatting.GREEN))
                                                .setCallback(
                                                        ((index2, clickType2, actionType2) -> {
                                                            if (user.rank == User.Rank.LEADER
                                                                    && (targetUser.rank
                                                                                    == User.Rank
                                                                                            .LEADER
                                                                            || targetUser.rank
                                                                                    == User.Rank
                                                                                            .OWNER)) {
                                                                new Message(
                                                                                "Cannot kick"
                                                                                    + " members"
                                                                                    + " with a"
                                                                                    + " higher of"
                                                                                    + " equivalent"
                                                                                    + " rank")
                                                                        .format(Formatting.RED)
                                                                        .send(player, false);
                                                                return;
                                                            } // TODO: Translations

                                                            GuiInteract.playClickSound(player);
                                                            targetUser.leaveFaction();
                                                            new Message(
                                                                            Text.translatable(
                                                                                    "factions.gui.members.entry.manage.kick.result.actor",
                                                                                    targetPlayer
                                                                                            .getName()
                                                                                            .getString()))
                                                                    .send(player, false);

                                                            if (targetPlayer != null) {
                                                                new Message(
                                                                                Text.translatable(
                                                                                        "factions.gui.members.entry.manage.kick.result.subject",
                                                                                        player.getName()
                                                                                                .getString()))
                                                                        .send(targetPlayer, false);
                                                            }
                                                            this.open();
                                                        })));
                                gui.setSlot(
                                        3,
                                        new GuiElementBuilder(Items.STRUCTURE_VOID)
                                                .setName(
                                                        Text.translatable(
                                                                        "factions.gui.members.entry.manage.kick.confirm.no")
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
                                    Text.translatable(
                                                    "factions.gui.members.entry.info.rank",
                                                    Text.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())
                                                            .setStyle(
                                                                    Style.EMPTY
                                                                            .withItalic(false)
                                                                            .withColor(
                                                                                    Formatting
                                                                                            .GREEN)))
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withItalic(false)
                                                            .withColor(Formatting.GRAY)));
                            icon.setLore(lore);
                        });
            }
            icon.setLore(lore);

            return DisplayElement.of(icon);
        }
        return DisplayElement.empty();
    }
}
