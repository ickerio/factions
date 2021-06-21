package io.icker.factions.database;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.util.Formatting;

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
        Query query = new Query("INSERT INTO Faction VALUES (?, ?, ?, ?, ?);")
            .set(name, description, color, open, power)
            .executeUpdate();

        if (!query.success) return null;
        return new Faction(name, description, Formatting.byName(color), open, power);
    }

    public Faction(String name, String description, Formatting color, boolean open, int power) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.open = open;
        this.power = power;
    }

    public int getClaimCount() {
        return new Query("SELECT COUNT(*) AS count FROM Claim where faction = ?;")
            .set(name)
            .executeQuery()
            .getInt("count");
    }

    public Claim claim(int x, int z, String level) {
        return Claim.add(x, z, level, name);
    }
    
    public Member addMember(UUID uuid) {
        return Member.add(uuid, name);
    }

    public ArrayList<Member> getMembers() {
        Query query = new Query("SELECT uuid FROM Member WHERE faction = ?;")
            .set(name)
            .executeQuery();

        if (!query.success) return null;
        ArrayList<Member> members = new ArrayList<Member>();

        while (query.next()) {
            members.add(new Member((UUID) query.getObject("uuid"), name));
        }
        return members;
    }

    public void setDescription(String description) {
        new Query("UPDATE Faction SET description = ? WHERE name = ?;")
            .set(description, name)
            .executeUpdate();
    }

    public void setColor(Formatting color) {
        new Query("UPDATE Faction SET color = ? WHERE name = ?;")
            .set(color.getName(), name)
            .executeUpdate();
    }

    public void setOpen(boolean open) {
        new Query("UPDATE Faction SET open = ? WHERE name = ?;")
            .set(open, name)
            .executeUpdate();
    }

    public ArrayList<Invite> getInvites() {
        return Invite.get(name);
    }

    public Home getHome() {
        return Home.get(name);
    }

    public Home setHome(double x, double y, double z, float yaw, float pitch, String level) {
        return Home.set(name, x, y, z, yaw, pitch, level);
    }

    public void remove() {
        new Query("DELETE FROM Faction WHERE name = ?;")
            .set(name)
            .executeUpdate();
    }
}