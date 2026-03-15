package io.icker.factions.ui;

import com.mojang.authlib.GameProfile;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.GuiInteract;
import io.icker.factions.util.Icons;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.List;
import java.util.stream.Collectors;

public class InfoGui extends SimpleGui {
    protected final Runnable closeCallback;

    public InfoGui(ServerPlayer player, Faction faction, @Nullable Runnable closeCallback) {
        super(MenuType.GENERIC_9x1, player, false);
        this.closeCallback = closeCallback;

        MinecraftServer server = player.level().getServer();
        ProfileResolver resolver = server.services().profileResolver();

        User user = User.get(player.getUUID());
        boolean isMember = faction.equals(user.getFaction());
        List<User> members = faction.getUsers();

        String owner =
                members.stream()
                        .filter(u -> u.rank == User.Rank.OWNER)
                        .map(
                                u ->
                                        resolver.fetchById(u.getID())
                                                .orElse(
                                                        new GameProfile(
                                                                Util.NIL_UUID,
                                                                Localization.raw(
                                                                        "factions.gui.generic.unknown_player",
                                                                        player)))
                                                .name())
                        .collect(Collectors.joining(", "));

        this.setTitle(Component.translatable("factions.gui.info.title"));

        for (int i = 0; i < 9; i++)
            this.setSlot(i, new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());

        // Faction info
        this.setSlot(
                0,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(
                                isMember ? Icons.GUI_CASTLE_NORMAL : Icons.GUI_CASTLE_OPEN)
                        .setName(Component.literal(faction.getColor() + faction.getName()))
                        .setLore(
                                List.of(
                                        Component.literal(faction.getDescription())
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(ChatFormatting.WHITE)),
                                        Component.translatable(
                                                        "factions.gui.info.owner",
                                                        Component.literal(owner)
                                                                .setStyle(
                                                                        Style.EMPTY
                                                                                .withItalic(false)
                                                                                .withColor(
                                                                                        ChatFormatting
                                                                                                .YELLOW)))
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(ChatFormatting.GRAY)))));

        // Members info
        int maxSize = FactionsMod.CONFIG.MAX_FACTION_SIZE;
        boolean isMaxSize = FactionsMod.CONFIG.MAX_FACTION_SIZE > -1;

        List<Component> membersLore =
                new java.util.ArrayList<>(
                        members.stream()
                                .map(
                                        u ->
                                                Component.literal(
                                                                resolver.fetchById(u.getID())
                                                                        .orElse(
                                                                                new GameProfile(
                                                                                        Util
                                                                                                .NIL_UUID,
                                                                                        Localization
                                                                                                .raw(
                                                                                                        "factions.gui.generic.unknown_player",
                                                                                                        player)))
                                                                        .name())
                                                        .setStyle(
                                                                Style.EMPTY
                                                                        .withItalic(false)
                                                                        .withColor(
                                                                                ChatFormatting
                                                                                        .GRAY)))
                                .toList());
        if (membersLore.size() > 4) membersLore = membersLore.subList(0, 3);
        membersLore.add(Component.empty());
        membersLore.add(
                Component.translatable("factions.gui.info.members.viewall")
                        .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY)));

        this.setSlot(
                1,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_PLAYER)
                        .setName(
                                Component.translatable(
                                        "factions.gui.info.members",
                                        Component.literal(
                                                        members.size()
                                                                + (isMaxSize
                                                                        ? ("/" + maxSize)
                                                                        : ""))
                                                .withStyle(ChatFormatting.GREEN)))
                        .setLore(membersLore)
                        .setCallback(
                                () -> {
                                    GuiInteract.playClickSound(player);
                                    new MemberGui(player, faction, this::open);
                                }));

        // Power info
        int requiredPower = faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower = faction.calculateMaxPower();
        this.setSlot(
                2,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_FIST)
                        .setName(Component.translatable("factions.gui.info.power"))
                        .setLore(
                                List.of(
                                        Component.translatable(
                                                        "factions.gui.info.power.total",
                                                        Component.literal(
                                                                        String.valueOf(
                                                                                faction.getPower()))
                                                                .setStyle(
                                                                        Style.EMPTY
                                                                                .withItalic(false)
                                                                                .withColor(
                                                                                        ChatFormatting
                                                                                                .GREEN)))
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(ChatFormatting.GRAY)),
                                        Component.translatable(
                                                        "factions.gui.info.power.claims",
                                                        Component.literal(
                                                                        String.valueOf(
                                                                                requiredPower))
                                                                .setStyle(
                                                                        Style.EMPTY
                                                                                .withItalic(false)
                                                                                .withColor(
                                                                                        ChatFormatting
                                                                                                .GREEN)))
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(ChatFormatting.GRAY)),
                                        Component.translatable(
                                                        "factions.gui.info.power.max",
                                                        Component.literal(String.valueOf(maxPower))
                                                                .setStyle(
                                                                        Style.EMPTY
                                                                                .withItalic(false)
                                                                                .withColor(
                                                                                        ChatFormatting
                                                                                                .GREEN)))
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(ChatFormatting.GRAY)))));

        // Allies info
        List<Component> allies =
                faction.getMutualAllies().isEmpty()
                        ? List.of(
                                Component.translatable("factions.gui.info.allies.none")
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(ChatFormatting.GRAY)))
                        : faction.getMutualAllies().stream()
                                .map(rel -> Faction.get(rel.target))
                                .map(fac -> Component.nullToEmpty(fac.getColor() + fac.getName()))
                                .toList();
        this.setSlot(
                3,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_CASTLE_ALLY)
                        .setName(
                                Component.translatable(
                                                "factions.gui.info.allies.some",
                                                faction.getMutualAllies().size())
                                        .withStyle(ChatFormatting.GREEN))
                        .setLore(allies));
        // Enemies info
        List<Component> enemies =
                faction.getEnemiesWith().isEmpty()
                        ? List.of(
                                Component.translatable("factions.gui.info.enemies.none")
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(ChatFormatting.GRAY)))
                        : faction.getEnemiesWith().stream()
                                .map(rel -> Faction.get(rel.target))
                                .map(fac -> Component.nullToEmpty(fac.getColor() + fac.getName()))
                                .toList();
        this.setSlot(
                4,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setProfileSkinTexture(Icons.GUI_CASTLE_ENEMY)
                        .setName(
                                Component.translatable(
                                                "factions.gui.info.enemies.some",
                                                faction.getEnemiesWith().size())
                                        .withStyle(ChatFormatting.RED))
                        .setLore(enemies));

        if (Command.Requires.isOwner().test(player.createCommandSourceStack()) && isMember) {
            this.setSlot(
                    6,
                    new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setProfileSkinTexture(Icons.GUI_LECTERN)
                            .setName(Component.translatable("factions.gui.info.settings"))
                            .setLore(
                                    List.of(
                                            Component.translatable(
                                                            "factions.gui.info.settings.lore")
                                                    .withStyle(ChatFormatting.GRAY)))
                            .setCallback(
                                    () -> {
                                        GuiInteract.playClickSound(player);
                                        new ModifyGui(player, faction, this::open);
                                    }));
        }

        this.setSlot(
                8,
                new GuiElementBuilder(Items.STRUCTURE_VOID)
                        .setName(
                                Component.translatable(
                                                closeCallback == null
                                                        ? "factions.gui.generic.close"
                                                        : "factions.gui.generic.back")
                                        .withStyle(ChatFormatting.RED))
                        .setCallback(
                                () -> {
                                    GuiInteract.playClickSound(player);
                                    if (closeCallback == null) {
                                        this.close();
                                    } else {
                                        closeCallback.run();
                                    }
                                }));

        this.open();
    }

    @Override
    public void onClose() {
        if (closeCallback == null) {
            super.onClose();
            return;
        }
        closeCallback.run();
    }
}
