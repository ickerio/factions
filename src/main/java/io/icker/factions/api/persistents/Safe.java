package io.icker.factions.api.persistents;

import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import net.minecraft.block.ChestBlock;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.HashMap;
import java.util.UUID;

@Name("Safe")
public class Safe {

    private static final HashMap<String, Safe> STORE = Database.load(Safe.class, safe -> safe.factionName);

    @Field("Inventory")
    public SimpleInventory inventory = new SimpleInventory(54);

    @Field("FactionName")
    public String factionName;

    public Safe(String factionName){
        this.factionName = factionName;
    }

    public Safe(){}


    public String getKey() {
        return factionName;
    }

    public static Safe getSafe(String factionName){
        return STORE.get(factionName);
    }

    public static void add(Safe safe){
        STORE.put(safe.factionName, safe);
    }

    public static void save() {
        Database.save(Safe.class, STORE.values().stream().toList());
    }

    public static void saveBackup(){
        Database.saveBackup(Safe.class, STORE.values().stream().toList());
    }

    public void remove(){
        STORE.remove(this.factionName);
    }

}
