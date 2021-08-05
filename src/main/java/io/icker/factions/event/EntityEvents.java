package io.icker.factions.event;

import io.icker.factions.database.Claim;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.HashSet;
import java.util.Set;

public class EntityEvents {
    private static final Set<EntityType<?>> MONSTERS = new HashSet<>();
    private static final Set<EntityType<?>> ANIMALS = new HashSet<>();

    /**
     * Called when an entity tries to spawn.
     * @param entity The entity that is trying to spawn.
     * @return True if the entity can spawn. False if it cant.
     */
    public static boolean entitySpawn(Entity entity) {
        EntityType<?> type = entity.getType();
        if (MONSTERS.contains(type)) {
            Claim claim = Claim.get(entity.getChunkPos().x, entity.getChunkPos().z, entity.getEntityWorld().getRegistryKey().getValue().toString());
            if (claim == null)
                return true;

            return claim.getFaction().allowMonsters;
        } else if (ANIMALS.contains(type)) {
            Claim claim = Claim.get(entity.getChunkPos().x, entity.getChunkPos().z, entity.getEntityWorld().getRegistryKey().getValue().toString());
            if (claim == null)
                return true;

            return claim.getFaction().allowAnimals;
        }

        return true;
    }

    static {
        MONSTERS.add(EntityType.BLAZE);
        MONSTERS.add(EntityType.CAVE_SPIDER);
        MONSTERS.add(EntityType.CREEPER);
        MONSTERS.add(EntityType.DROWNED);
        MONSTERS.add(EntityType.ELDER_GUARDIAN); // Exception for (mini) boss mobs in the rule maybe?
        MONSTERS.add(EntityType.ENDER_DRAGON); // Exception for (mini) boss mobs in the rule maybe?
        MONSTERS.add(EntityType.ENDERMAN);
        MONSTERS.add(EntityType.ENDERMITE);
        MONSTERS.add(EntityType.EVOKER);
        MONSTERS.add(EntityType.GHAST);
        MONSTERS.add(EntityType.GIANT); // Do we even need to care?
        MONSTERS.add(EntityType.GUARDIAN);
        MONSTERS.add(EntityType.HOGLIN);
        MONSTERS.add(EntityType.HUSK);
        MONSTERS.add(EntityType.ILLUSIONER);
        MONSTERS.add(EntityType.MAGMA_CUBE);
        MONSTERS.add(EntityType.PHANTOM);
        MONSTERS.add(EntityType.PIGLIN);
        MONSTERS.add(EntityType.PIGLIN_BRUTE);
        MONSTERS.add(EntityType.PILLAGER);
        MONSTERS.add(EntityType.RAVAGER);
        MONSTERS.add(EntityType.SHULKER);
        MONSTERS.add(EntityType.SILVERFISH);
        MONSTERS.add(EntityType.SKELETON);
        MONSTERS.add(EntityType.SKELETON_HORSE);
        MONSTERS.add(EntityType.SLIME);
        MONSTERS.add(EntityType.SPIDER);
        MONSTERS.add(EntityType.STRAY);
        MONSTERS.add(EntityType.VEX);
        MONSTERS.add(EntityType.VINDICATOR);
        MONSTERS.add(EntityType.WITCH);
        MONSTERS.add(EntityType.WITHER); // Exception for (mini) boss mobs in the rule maybe?
        MONSTERS.add(EntityType.WITHER_SKELETON);
        MONSTERS.add(EntityType.ZOGLIN);
        MONSTERS.add(EntityType.ZOMBIE);
        MONSTERS.add(EntityType.ZOMBIE_HORSE);
        MONSTERS.add(EntityType.ZOMBIE_VILLAGER);
        MONSTERS.add(EntityType.ZOMBIFIED_PIGLIN);

        ANIMALS.add(EntityType.AXOLOTL);
        ANIMALS.add(EntityType.BAT);
        ANIMALS.add(EntityType.BEE); // Might cause side effects of bees not coming out of hives/nests anymore
        ANIMALS.add(EntityType.CAT);
        ANIMALS.add(EntityType.CHICKEN);
        ANIMALS.add(EntityType.COD);
        ANIMALS.add(EntityType.COW);
        ANIMALS.add(EntityType.DOLPHIN);
        ANIMALS.add(EntityType.DONKEY);
        ANIMALS.add(EntityType.FOX);
        ANIMALS.add(EntityType.GLOW_SQUID);
        ANIMALS.add(EntityType.GOAT);
        ANIMALS.add(EntityType.HORSE);
        ANIMALS.add(EntityType.IRON_GOLEM);
        ANIMALS.add(EntityType.LLAMA);
        ANIMALS.add(EntityType.MULE);
        ANIMALS.add(EntityType.MOOSHROOM);
        ANIMALS.add(EntityType.OCELOT);
        ANIMALS.add(EntityType.PANDA);
        ANIMALS.add(EntityType.PARROT);
        ANIMALS.add(EntityType.PIG);
        ANIMALS.add(EntityType.POLAR_BEAR);
        ANIMALS.add(EntityType.PUFFERFISH);
        ANIMALS.add(EntityType.RABBIT);
        ANIMALS.add(EntityType.SALMON);
        ANIMALS.add(EntityType.SHEEP);
        ANIMALS.add(EntityType.SNOW_GOLEM);
        ANIMALS.add(EntityType.SQUID);
        ANIMALS.add(EntityType.STRIDER);
        ANIMALS.add(EntityType.TRADER_LLAMA);
        ANIMALS.add(EntityType.TROPICAL_FISH);
        ANIMALS.add(EntityType.TURTLE);
        ANIMALS.add(EntityType.VILLAGER);
        ANIMALS.add(EntityType.WANDERING_TRADER);
        ANIMALS.add(EntityType.WOLF);

        /*
        AREA_EFFECT_CLOUD
        ARMOR_STAND
        ARROW
        BOAT
        DRAGON_FIREBALL
        END_CRYSTAL
        EVOKER_FANGS
        EXPERIENCE_ORB
        EYE_OF_ENDER
        FALLING_BLOCK
        FIREWORK_ROCKET
        GLOW_ITEM_FRAME
        ITEM
        ITEM_FRAME
        FIREBALL
        LEASH_KNOT
        LIGHTNING_BOLT
        LLAMA_SPIT
        MARKER
        MINECART
        CHEST_MINECART
        COMMAND_BLOCK_MINECART
        FURNACE_MINECART
        HOPPER_MINECART
        SPAWNER_MINECART
        TNT_MINECART
        PAINTING
        TNT
        SHULKER_BULLET
        SMALL_FIREBALL
        SNOWBALL
        SPECTRAL_ARROW
        EGG
        ENDER_PEARL
        EXPERIENCE_BOTTLE
        TRIDENT
        WITHER_SKULL
        PLAYER
        FISHING_BOBBER
         */
    }
}
