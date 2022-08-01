package io.icker.factions.api.persistents;

import io.icker.factions.FactionsMod;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Name("War")
public class War {
    private static final HashMap<UUID, War> STORE = Database.load(War.class, War::getID);

    @Field("ID")
    private UUID id;

    @Field("Source")
    private UUID source;

    @Field("Target")
    private UUID target;

    @Field("SourceTeam")
    private ArrayList<UUID> sourceTeam = new ArrayList<>();

    @Field("TargetTeam")
    private ArrayList<UUID> targetTeam = new ArrayList<>();

    @Field("Name")
    private String name;

    public boolean sourceReadyToEnd = false;
    public boolean targetReadyToEnd = false;

    public War(Faction source, Faction target) {
        this.source = source.getID();
        this.target = target.getID();
        this.targetTeam.add(target.getID());
        this.sourceTeam.add(source.getID());
        this.id = UUID.randomUUID();
        name = String.format("The %s-%s war", source.getName(), target.getName());

        for (Faction faction : getFactions()) {
            for (User user : faction.getUsers()) {
                user.lives = FactionsMod.CONFIG.WAR.NUM_LIVES;
            }
        }
    }

    public War() {}

    public String getKey() {
        return id.toString();
    }

    public static War get(UUID id) {
        return STORE.get(id);
    }

    public static War getByFactions(Faction source, Faction target) {
        UUID id = STORE
            .keySet()
            .stream()
            .filter(warKey -> {
                War war = get(warKey);
                return (war.getSourceTeam().contains(source) && war.getTargetTeam().contains(target)) || (war.getSourceTeam().contains(target) && war.getTargetTeam().contains(source));
            })
            .findFirst()
            .orElse(null);

        if (id == null) return null;
        return War.get(id);
    }

    public static List<War> getByFaction(Faction source) {
        return STORE
                .keySet()
                .stream()
                .filter(warKey -> {
                    War war = get(warKey);
                    return war.getSourceTeam().contains(source) || war.getTargetTeam().contains(source);
                })
                .map(War::get)
                .toList();

    }

    public static War getByName(String name) {
        FactionsMod.LOGGER.info(all().size());
        return all().stream().filter(war -> war.getName().equals(name)).findFirst().orElse(null);
    }

    public static List<War> all() {
        return STORE.keySet().stream().map(War::get).toList();
    }

    public static void add(War war) {
        STORE.put(war.getID(), war);
    }

    public UUID getID() {
        return id;
    }

    public void addSource(Faction source) {
        for (User user : source.getUsers()) {
            user.lives = FactionsMod.CONFIG.WAR.NUM_LIVES;
        }
        this.sourceTeam.add(source.getID());
    }

    public List<Faction> getSourceTeam() {
        return sourceTeam.stream().map(Faction::get).toList();
    }

    public Faction getSource() {
        return Faction.get(source);
    }

    public void addTarget(Faction target) {
        for (User user : target.getUsers()) {
            user.lives = FactionsMod.CONFIG.WAR.NUM_LIVES;
        }
        this.targetTeam.add(target.getID());
    }

    public List<Faction> getTargetTeam() {
        return targetTeam.stream().map(Faction::get).toList();
    }

    public Faction getTarget() {
        return Faction.get(target);
    }

    public String getName() {
        return name;
    }

    public List<Faction> getFactions() {
        return Stream.concat(getTargetTeam().stream(), getSourceTeam().stream()).toList();
    }

    public void end() {
        STORE.remove(this.getID());
        for (Faction faction : getFactions()) {
            for (User user : faction.getUsers()) {
                user.lives = FactionsMod.CONFIG.WAR.NUM_LIVES;
            }
        }
    }

    public static void save() {
        Database.save(War.class, STORE.values().stream().toList());
    }
}
