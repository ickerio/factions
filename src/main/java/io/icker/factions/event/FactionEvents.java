package io.icker.factions.event;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Claim;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.Formatting;

public class FactionEvents {
    public static void playerDeath(ServerPlayerEntity player) {
        Member member = Member.get(player.getUuid());
        if (member == null) return;

        Faction faction = member.getFaction();

        int adjusted = adjustPower(faction, -Config.POWER_DEATH_PENALTY);
        new Message("%s lost %d power from dying", player.getName().asString(), adjusted).send(faction);
    }

    public static void tick(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        String dimension = world.getRegistryKey().getValue().toString();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();

        Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (claim != null) {
            new Message(claim.getFaction().name)
                .format(claim.getFaction().color)
                .send(player, true);
        } else {
            new Message("Wilderness")
                .format(Formatting.GREEN)
                .send(player, true);
        }
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
