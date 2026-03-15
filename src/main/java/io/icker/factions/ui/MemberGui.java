package io.icker.factions.ui;

import com.mojang.authlib.GameProfile;

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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MemberGui extends PagedGui {
    Faction faction;
    int size;
    ProfileResolver resolver;
    User user;

    List<User> members;

    public MemberGui(ServerPlayer player, Faction faction, @Nullable Runnable closeCallback) {
        super(player, closeCallback);
        this.faction = faction;
        this.resolver = player.level().getServer().services().profileResolver();
        this.user = User.get(player.getUUID());

        this.members = new ArrayList<>(faction.getUsers());
        if (faction.equals(this.user.getFaction())) {
            this.members.remove(user);
            this.members.addFirst(user);
        }
        this.size = members.size();

        this.setTitle(
                Component.translatable(
                        "factions.gui.members.title",
                        Component.literal(faction.getColor() + faction.getName()),
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

            GameProfile profile = resolver.fetchById(targetUser.getID()).orElse(unknownPlayer);

            var icon = new GuiElementBuilder(Items.PLAYER_HEAD);
            icon.setProfile(profile);
            icon.setName(Component.literal(profile.name()));

            if (profile.equals(unknownPlayer)) {
                List<Component> lore =
                        List.of(
                                Component.translatable("factions.gui.members.entry.unknown_player")
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(ChatFormatting.GRAY)));
                icon.setLore(lore);
                icon.setProfileSkinTexture(Icons.GUI_UNKNOWN_PLAYER);
                return DisplayElement.of(icon);
            }

            List<Component> lore =
                    new ArrayList<>(
                            List.of(
                                    Component.translatable(
                                                    "factions.gui.members.entry.info.rank",
                                                    Component.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())
                                                            .setStyle(
                                                                    Style.EMPTY
                                                                            .withItalic(false)
                                                                            .withColor(
                                                                                    ChatFormatting
                                                                                            .GREEN)))
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withItalic(false)
                                                            .withColor(ChatFormatting.GRAY))));

            if (!profile.id().equals(player.getUUID())
                    && Command.Requires.isLeader().test(player.createCommandSourceStack())
                    && Command.Requires.hasPerms("factions.rank.promote", 0)
                            .test(player.createCommandSourceStack())
                    && faction.equals(user.getFaction())) {
                lore.add(
                        Component.translatable("factions.gui.members.entry.manage.promote")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(ChatFormatting.DARK_GREEN)));
                lore.add(
                        Component.translatable("factions.gui.members.entry.manage.demote")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(ChatFormatting.DARK_RED)));
                lore.add(
                        Component.translatable("factions.gui.members.entry.manage.kick")
                                .setStyle(
                                        Style.EMPTY
                                                .withItalic(false)
                                                .withColor(ChatFormatting.DARK_RED)));
                ServerPlayer targetPlayer =
                        player.level().getServer().getPlayerList().getPlayer(targetUser.getID());
                icon.setCallback(
                        (index, clickType, actionType) -> {
                            GuiInteract.playClickSound(player);
                            if (clickType == ClickType.MOUSE_LEFT) {
                                try {
                                    RankCommand.execPromote(targetUser, player);
                                    new Message(
                                                    Component.translatable(
                                                            "factions.gui.members.entry.manage.promote.result",
                                                            profile.name(),
                                                            Component.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())))
                                            .prependFaction(faction)
                                            .send(player, false);
                                } catch (Exception e) {
                                    new Message(e.getMessage())
                                            .format(ChatFormatting.RED)
                                            .send(player, false);
                                    return;
                                }
                            }
                            if (clickType == ClickType.MOUSE_RIGHT) {
                                try {
                                    RankCommand.execDemote(targetUser, player);

                                    new Message(
                                                    Component.translatable(
                                                            "factions.gui.members.entry.manage.demote.result",
                                                            profile.name(),
                                                            Component.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())))
                                            .prependFaction(faction)
                                            .send(player, false);
                                } catch (Exception e) {
                                    new Message(e.getMessage())
                                            .format(ChatFormatting.RED)
                                            .send(player, false);
                                    return;
                                }
                            }
                            if (clickType == ClickType.DROP) {
                                SimpleGui gui = new SimpleGui(MenuType.HOPPER, player, false);
                                for (int i = 0; i < 5; i++)
                                    gui.setSlot(
                                            i,
                                            new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                                                    .hideTooltip());
                                gui.setTitle(
                                        Component.translatable(
                                                "factions.gui.members.entry.manage.kick.confirm.title"));
                                gui.setSlot(
                                        1,
                                        new GuiElementBuilder(Items.SLIME_BALL)
                                                .setName(
                                                        Component.translatable(
                                                                        "factions.gui.members.entry.manage.kick.confirm.yes",
                                                                        profile.name())
                                                                .withStyle(ChatFormatting.GREEN))
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
                                                                                Component
                                                                                        .translatable(
                                                                                                "factions.command.kick.fail.high_rank"))
                                                                        .format(ChatFormatting.RED)
                                                                        .send(player, false);
                                                                return;
                                                            }

                                                            GuiInteract.playClickSound(player);
                                                            targetUser.leaveFaction();
                                                            new Message(
                                                                            Component.translatable(
                                                                                    "factions.gui.members.entry.manage.kick.result.actor",
                                                                                    profile.name()))
                                                                    .send(player, false);

                                                            if (targetPlayer != null) {
                                                                new Message(
                                                                                Component
                                                                                        .translatable(
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
                                                        Component.translatable(
                                                                        "factions.gui.members.entry.manage.kick.confirm.no")
                                                                .withStyle(ChatFormatting.RED))
                                                .setCallback(
                                                        () -> {
                                                            GuiInteract.playClickSound(player);
                                                            this.open();
                                                        }));
                                gui.open();
                            }
                            lore.removeFirst();
                            lore.addFirst(
                                    Component.translatable(
                                                    "factions.gui.members.entry.info.rank",
                                                    Component.translatable(
                                                                    "factions.gui.members.entry.info.rank."
                                                                            + targetUser
                                                                                    .getRankName())
                                                            .setStyle(
                                                                    Style.EMPTY
                                                                            .withItalic(false)
                                                                            .withColor(
                                                                                    ChatFormatting
                                                                                            .GREEN)))
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withItalic(false)
                                                            .withColor(ChatFormatting.GRAY)));
                            icon.setLore(lore);
                        });
            }
            icon.setLore(lore);

            return DisplayElement.of(icon);
        }
        return DisplayElement.empty();
    }
}
