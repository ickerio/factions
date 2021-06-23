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

        int adjusted = adjustPower(faction, Config.POWER_DEATH_PENALTY);
        new Message("Lost " + adjusted + " power from member death").send(faction);
    }

    public static void powerTick(ServerPlayerEntity player) {
        Member member = Member.get(player.getUuid());
        if (member == null) return;

        Faction faction = member.getFaction();

        int adjusted = adjustPower(faction, Config.TICKS_FOR_POWER_REWARD);
        if (adjusted != 0) new Message("Gained " + adjusted + " power for member survival").send(faction);
    }

    public static int adjustPower(Faction faction, int adjustment) {
        int maxPower = Config.BASE_POWER + (faction.getMembers().size() * Config.MEMBER_POWER);

        int result = faction.power + adjustment > maxPower ? maxPower : faction.power + adjustment;
        faction.setPower(result);
        return result - faction.power;
    }
}
