package io.icker.factions.database;

import io.icker.factions.FactionsMod;
import io.icker.factions.event.FactionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Faction {
    public String name;
    public String description;
    public Formatting color;
    public boolean open;
    public int power;

    public static Faction get(String name) {
        Query query = new Query("SELECT * FROM Faction WHERE name = ?;")
            .set(name)
            .executeQuery();

        if (!query.success) return null;
        return new Faction(name, query.getString("description"), Formatting.byName(query.getString("color")), query.getBool("open"), query.getInt("power"));
    }

    public static Faction add(String name, String description, String color, boolean open, int power) {
        Query query = new Query("INSERT INTO Faction (name, description, color, open, power) VALUES (?, ?, ?, ?, ?);")
            .set(name, description, color, open, power)
            .executeUpdate();

        if (!query.success) return null;
        return new Faction(name, description, Formatting.byName(color), open, power);
    }

    public static ArrayList<Faction> all() {
        Query query = new Query("SELECT * FROM Faction;")
            .executeQuery();

        ArrayList<Faction> factions = new ArrayList<Faction>();
        if (!query.success) return factions;

        while (query.next()) {
            factions.add(new Faction(query.getString("name"), query.getString("description"), Formatting.byName(query.getString("color")), query.getBool("open"), query.getInt("power")));
        }
        return factions;
    }

    public static ArrayList<Faction> allBut(String faction) {
        Query query = new Query("SELECT * FROM Faction WHERE NOT name = ?;")
            .set(faction)
            .executeQuery();

        ArrayList<Faction> factions = new ArrayList<Faction>();
        if (!query.success) return factions;

        while (query.next()) {
            factions.add(new Faction(query.getString("name"), query.getString("description"), Formatting.byName(query.getString("color")), query.getBool("open"), query.getInt("power")));
        }
        return factions;
    }

    public Faction(String name, String description, Formatting color, boolean open, int power) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.open = open;
        this.power = power;
    }

    public void setDescription(String description) {
        new Query("UPDATE Faction SET description = ? WHERE name = ?;")
            .set(description, name)
            .executeUpdate();

        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.updateFaction(this, Faction.get(this.name));
        }
    }

    public void setColor(Formatting color) {
        new Query("UPDATE Faction SET color = ? WHERE name = ?;")
            .set(color.getName(), name)
            .executeUpdate();

        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.updateFaction(this, Faction.get(this.name));
        }
        List<ServerPlayerEntity> players = this.getMembers().stream().map(member -> FactionsMod.playerManager.getPlayer(member.uuid)).toList();
        FactionEvents.updatePlayerList(players);
    }

    public void setOpen(boolean open) {
        new Query("UPDATE Faction SET open = ? WHERE name = ?;")
            .set(open, name)
            .executeUpdate();
    }

    public void setPower(int power) {
        new Query("UPDATE Faction SET power = ? WHERE name = ?;")
            .set(power, name)
            .executeUpdate();

        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.updateFaction(this, Faction.get(this.name));
        }
    }

    public ArrayList<Member> getMembers() {
        Query query = new Query("SELECT uuid, rank FROM Member WHERE faction = ?;")
            .set(name)
            .executeQuery();

        ArrayList<Member> members = new ArrayList<Member>();
        if (!query.success) return members;

        while (query.next()) {
            members.add(new Member((UUID) query.getObject("uuid"), name, Member.Rank.valueOf(query.getString("rank").toUpperCase())));
        }
        return members;
    }

    public Member addMember(UUID uuid) {
        Member member = Member.add(uuid, name);

        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.updateFaction(this, Faction.get(this.name));
        }

        return member;
    }

    public Member addMember(UUID uuid, Member.Rank rank) {
        Member member = Member.add(uuid, name, rank);

        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.updateFaction(this, Faction.get(this.name));
        }

        return member;
    }

    public ArrayList<Claim> getClaims() {
        Query query = new Query("SELECT * FROM Claim WHERE faction = ?;")
            .set(name)
            .executeQuery();

        ArrayList<Claim> claims = new ArrayList<Claim>();
        if (!query.success) return claims;

        while (query.next()) {
            claims.add(new Claim(query.getInt("x"), query.getInt("z"), query.getString("level"), name));
        }
        return claims;
    }

    public void removeAllClaims() {
        new Query("DELETE FROM Claim WHERE faction = ?;")
            .set(name)
            .executeUpdate();

        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.removeAll(this);
        }
    }

    public Claim addClaim(int x, int z, String level) {
        return Claim.add(x, z, level, name);
    }

    public ArrayList<Invite> getInvites() {
        return Invite.get(name);
    }

    public Home getHome() {
        return Home.get(name);
    }

    public Home setHome(double x, double y, double z, float yaw, float pitch, String level) {
        Home home = Home.set(name, x, y, z, yaw, pitch, level);

        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.setHome(this, home);
        }

        return home;
    }

    public void remove() {
        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.removeHome(this);
            FactionsMod.dynmap.removeAll(this);
        }

        List<ServerPlayerEntity> players = this.getMembers().stream().map(member -> FactionsMod.playerManager.getPlayer(member.uuid)).toList();

        new Query("DELETE FROM Faction WHERE name = ?;")
            .set(name)
            .executeUpdate();

        FactionEvents.updatePlayerList(players);
    }
}