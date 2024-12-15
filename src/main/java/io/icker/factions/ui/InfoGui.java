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
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class InfoGui extends SimpleGui {
    protected final Runnable closeCallback;

    /**
     * Constructs a new simple container gui for the supplied player.
     *
     * @param player the player to server this gui to
     *               will be treated as slots of this gui
     */
    public InfoGui(ServerPlayerEntity player, Faction faction, @Nullable Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        this.closeCallback = closeCallback;

        MinecraftServer server = player.getServer();
        UserCache cache = server.getUserCache();

        User user = User.get(player.getUuid());
        boolean isMember = faction == user.getFaction();
        List<User> members = faction.getUsers();

        String owner = members.stream().filter(u -> u.rank == User.Rank.OWNER)
                .map(u -> cache.getByUuid(u.getID())
                        .orElse(new GameProfile(Util.NIL_UUID, "{Unknown Player}"))
                        .getName()
                )
                .collect(Collectors.joining(", "));

        this.setTitle(Text.literal("Faction info"));

        for (int i = 0; i < 9; i++)
            this.setSlot(i, new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());

        // Faction info
        this.setSlot(0, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(isMember ? Icons.GUI_CASTLE_NORMAL : Icons.GUI_CASTLE_OPEN)
                .setName(Text.literal(faction.getColor() + faction.getName()))
                .setLore(List.of(
                                Text.literal(faction.getDescription())
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(Formatting.WHITE)
                                        ),
                                Text.literal("Owner: ")
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(Formatting.GRAY)
                                        ).append(
                                                Text.literal(owner).setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.YELLOW)
                                                )
                                        )
                        )
                )
        );

        // Members info
        int maxSize = FactionsMod.CONFIG.MAX_FACTION_SIZE;
        boolean isMaxSize = FactionsMod.CONFIG.MAX_FACTION_SIZE > -1;

        List<Text> membersLore = new java.util.ArrayList<>(members.stream()
                .map(u -> Text.literal(cache.getByUuid(u.getID())
                                .orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}"))
                                .getName())
                        .setStyle(
                                Style.EMPTY
                                        .withItalic(false)
                                        .withColor(Formatting.GRAY)
                        )
                )
                .toList()
        );
        if (membersLore.size() > 4) membersLore = membersLore.subList(0, 3);
        membersLore.add(Text.empty());
        membersLore.add(
                Text.literal("Click to view all members")
                        .setStyle(
                                Style.EMPTY
                                        .withItalic(false)
                                        .withColor(Formatting.GRAY)
                        )
        );

        this.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_PLAYER)
                .setName(Text.literal("Members ").append(Text.literal("(" + members.size() + (isMaxSize ? "/" + maxSize : "") + ")").formatted(Formatting.GREEN)))
                .setLore(membersLore)
                .setCallback(() -> {
                    GuiInteract.playClickSound(player);
                    new MemberGui(player, faction, this::open);
                })
        );

        // Power info
        int requiredPower = faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower = members.size() * FactionsMod.CONFIG.POWER.MEMBER + FactionsMod.CONFIG.POWER.BASE;
        this.setSlot(2, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_FIST)
                .setName(Text.literal("Faction Powers"))
                .setLore(
                        List.of(
                                Text.literal(String.valueOf(faction.getPower()))
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(Formatting.GREEN)
                                        )
                                        .append(Text.literal(" faction power")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY)
                                                )
                                        ),
                                Text.literal(String.valueOf(requiredPower))
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(Formatting.GREEN)
                                        )
                                        .append(Text.literal(" claims required power")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY)
                                                )
                                        ),
                                Text.literal(String.valueOf(maxPower))
                                        .setStyle(
                                                Style.EMPTY
                                                        .withItalic(false)
                                                        .withColor(Formatting.GREEN)
                                        )
                                        .append(Text.literal(" max faction power")
                                                .setStyle(
                                                        Style.EMPTY
                                                                .withItalic(false)
                                                                .withColor(Formatting.GRAY)
                                                )
                                        )
                        )
                )
        );

        // Allies info
        List<Text> allies = faction.getMutualAllies().isEmpty() ?
                List.of(Text.literal("No allies.")
                        .setStyle(
                                Style.EMPTY
                                        .withItalic(false)
                                        .withColor(Formatting.GRAY)
                        )
                ) :
                faction.getMutualAllies().stream()
                        .map(rel -> Faction.get(rel.target))
                        .map(fac -> Text.of(fac.getColor() + fac.getName()))
                        .toList();
        this.setSlot(3, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_CASTLE_ALLY)
                .setName(Text.literal("Ally factions ").append(Text.literal(String.format("(%s)", faction.getMutualAllies().size())).formatted(Formatting.GREEN)))
                .setLore(allies)
        );
        // Enemies info
        List<Text> enemies = faction.getEnemiesWith().isEmpty() ?
                List.of(Text.literal("No enemies.")
                        .setStyle(
                                Style.EMPTY
                                        .withItalic(false)
                                        .withColor(Formatting.GRAY)
                        )
                ) :
                faction.getEnemiesWith().stream()
                        .map(rel -> Faction.get(rel.target))
                        .map(fac -> Text.of(fac.getColor() + fac.getName()))
                        .toList();
        this.setSlot(4, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(Icons.GUI_CASTLE_ENEMY)
                .setName(Text.literal("Enemy factions ").append(Text.literal(String.format("(%s)", faction.getEnemiesWith().size())).formatted(Formatting.GREEN)))
                .setLore(enemies)
        );

        if (Command.Requires.isOwner().test(player.getCommandSource())) {
            this.setSlot(6, new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setSkullOwner(Icons.GUI_LECTERN)
                    .setName(Text.literal("Faction settings"))
                    .setLore(List.of(
                            Text.literal("Click to change settings").formatted(Formatting.GRAY)
                    ))
                    .setCallback(() -> {
                        GuiInteract.playClickSound(player);
                        new ModifyGui(player, faction, this::open);
                    })
            );
        }

        this.setSlot(8, new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Text.literal(closeCallback == null ? "Close" : "Go back").formatted(Formatting.RED))
                .setCallback(() -> {
                    GuiInteract.playClickSound(player);
                    if (closeCallback == null) {
                        this.close();
                    } else {
                        closeCallback.run();
                    }
                })
        );

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
