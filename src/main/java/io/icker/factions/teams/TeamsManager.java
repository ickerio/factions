package io.icker.factions.teams;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Formatting;

public class TeamsManager {
    private static final File configDir = FabricLoader.getInstance().getGameDir().resolve("factions").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ArrayList<Team> teams;

    public static Team getTeam(String name) {
        for (Team team : teams) {
            if (team.name == name) return team;
        }
        return null;
    }

    public static Team addTeam(String name, Formatting color) {
        Team newTeam = new Team(name, color);
        teams.add(newTeam);
        return newTeam;
    }

    public static Member getMember(UUID uuid) {
        for (Team team : teams) {
            for (Member member : team.members) {
                if (member.uuid.equals(uuid)) return member;
            }
        }
        return null;
    }

    public static Claim getClaim(int x, int z, String level) {
        for (Team team : teams) {
            for (Claim claim : team.claims) {
                if (claim.x == x & claim.z == z & claim.level == level) {
                    return claim;
                }
            }
        }
        return null;
    }

    public static void load() {
        File config = new File(configDir, "teams.json");

        if (config.exists()) {
            try {
                FileReader reader = new FileReader(config);
                teams = GSON.fromJson(reader, new TypeToken<ArrayList<Team>>() {}.getType());
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            teams = new ArrayList<Team>();
        }

        for (Team team : teams) {
            for (Member member : team.members) {
                member.team = team;
            }
            for (Claim claim : team.claims) {
                claim.owner = team;
            }
        }
    }

    public static void save() {
        try {
            configDir.mkdirs();
            File config = new File(configDir, "teams.json");
            FileWriter writer = new FileWriter(config);
            GSON.toJson(teams, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}