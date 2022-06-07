package io.icker.factions.database;

import java.util.HashMap;
import java.util.UUID;

import io.icker.factions.api.persistents.User.ChatMode;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.api.persistents.Relationship.Status;
import net.minecraft.nbt.NbtCompound;

public class TypeSerializerRegistry {
    private static final HashMap<Class<?>, TypeSerializer<?>> registry = new HashMap<Class<?>, TypeSerializer<?>>();

    static {
        registry.put(byte.class, new TypeSerializer<>(NbtCompound::putByte, NbtCompound::getByte));
        registry.put(short.class, new TypeSerializer<>(NbtCompound::putShort, NbtCompound::getShort));
        registry.put(int.class, new TypeSerializer<>(NbtCompound::putInt, NbtCompound::getInt));
        registry.put(long.class, new TypeSerializer<>(NbtCompound::putLong, NbtCompound::getLong));
        registry.put(UUID.class, new TypeSerializer<>(NbtCompound::putUuid, NbtCompound::getUuid));
        registry.put(float.class, new TypeSerializer<>(NbtCompound::putFloat, NbtCompound::getFloat));
        registry.put(double.class, new TypeSerializer<>(NbtCompound::putDouble, NbtCompound::getDouble));
        registry.put(String.class, new TypeSerializer<>(NbtCompound::putString, NbtCompound::getString));
        registry.put(byte[].class, new TypeSerializer<>(NbtCompound::putByteArray, NbtCompound::getByteArray));
        registry.put(int[].class, new TypeSerializer<>(NbtCompound::putIntArray, NbtCompound::getIntArray));
        registry.put(long[].class, new TypeSerializer<>(NbtCompound::putLongArray, NbtCompound::getLongArray));
        registry.put(boolean.class, new TypeSerializer<>(NbtCompound::putBoolean, NbtCompound::getBoolean));
        
        registry.put(ChatMode.class, createEnumSerializer(ChatMode.class));
        registry.put(Rank.class, createEnumSerializer(Rank.class));
        registry.put(Status.class, createEnumSerializer(Status.class));
    }

    public static TypeSerializer<?> get(Class<?> clazz) {
        return registry.get(clazz);
    }

    private static <T extends java.lang.Enum<T>> TypeSerializer<T> createEnumSerializer(Class<T> clazz) {
        return new TypeSerializer<T>(
            (compound, key, item) -> compound.putString(key, item.toString()),
            (compound, key) -> Enum.valueOf(clazz, compound.getString(key))
        );
    }

    // private static TypeSerializer<String[]> createStringArray() {
    //     return new TypeSerializer<String[]>(
    //         (compound, key, items) -> {
    //             NbtList list = new NbtList();
    //             for (String item : items) list.add(NbtString.of(item));
    //             compound.put(key, list);
    //         },
    //         (compound, key) -> {
    //             NbtList list = (NbtList)compound.get(key);
    //             String[] items = new String[list.size()];
    //             for (int i = 0; i < list.size(); i++) {
    //                 items[i] = list.getString(i);
    //             }
    //             return items;
    //         },
    //         new String[0]
    //     );
    // }
}
