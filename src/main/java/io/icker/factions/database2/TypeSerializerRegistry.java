package io.icker.factions.database2;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;

public class TypeSerializerRegistry {
    private static final HashMap<Class<?>, TypeSerializer<?>> registry = new HashMap<Class<?>, TypeSerializer<?>>();

    static {
        registry.put(byte.class, new TypeSerializer<>(NbtCompound::putByte, NbtCompound::getByte, (byte)0));
        registry.put(short.class, new TypeSerializer<>(NbtCompound::putShort, NbtCompound::getShort, (short)0));
        registry.put(int.class, new TypeSerializer<>(NbtCompound::putInt, NbtCompound::getInt, 0));
        registry.put(long.class, new TypeSerializer<>(NbtCompound::putLong, NbtCompound::getLong, 0L));
        registry.put(UUID.class, new TypeSerializer<>(NbtCompound::putUuid, NbtCompound::getUuid, UUID.randomUUID()));
        registry.put(float.class, new TypeSerializer<>(NbtCompound::putFloat, NbtCompound::getFloat, 0f));
        registry.put(double.class, new TypeSerializer<>(NbtCompound::putDouble, NbtCompound::getDouble, 0.0));
        registry.put(String.class, new TypeSerializer<>(NbtCompound::putString, NbtCompound::getString, ""));
        registry.put(byte[].class, new TypeSerializer<>(NbtCompound::putByteArray, NbtCompound::getByteArray, new byte[0]));
        registry.put(int[].class, new TypeSerializer<>(NbtCompound::putIntArray, NbtCompound::getIntArray, new int[0]));
        registry.put(long[].class, new TypeSerializer<>(NbtCompound::putLongArray, NbtCompound::getLongArray, new long[0]));
        registry.put(boolean.class, new TypeSerializer<>(NbtCompound::putBoolean, NbtCompound::getBoolean, false));
    }

    public static TypeSerializer<?> get(Class<?> clazz) {
        return registry.get(clazz);
    }
}
