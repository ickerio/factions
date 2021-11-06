package io.icker.factions.event;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;

public class FactionEvents {
    public static void playerDeath(ServerPlayerEntity player) {
        Member member = Member.get(player.getUuid());
        if (member == null) return;

        Faction faction = member.getFaction();

        int adjusted = adjustPower(faction, -Config.POWER_DEATH_PENALTY);
        new Message("%s lost %d power from dying", player.getName().asString(), adjusted).send(faction);
    }

    public static void powerTick(ServerPlayerEntity player) {
        Member member = Member.get(player.getUuid());
        if (member == null) return;

        Faction faction = member.getFaction();

        int adjusted = adjustPower(faction, Config.TICKS_FOR_POWER_REWARD);
        if (adjusted != 0) new Message("%s gained %d power from surviving", player.getName().asString(), adjusted).send(faction);
    }

    public static int adjustPower(Faction faction, int adjustment) {
        int maxPower = Config.BASE_POWER + (faction.getMembers().size() * Config.MEMBER_POWER);

        int updated = Math.min(Math.max(0, faction.power + adjustment), maxPower);
        faction.setPower(updated);
        return Math.abs(updated - faction.power);
    }
}
