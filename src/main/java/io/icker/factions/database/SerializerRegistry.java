package io.icker.factions.database;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

import io.icker.factions.api.persistents.Relationship.Status;
import io.icker.factions.api.persistents.User.ChatMode;
import io.icker.factions.api.persistents.User.Rank;
import net.minecraft.nbt.*;

public class SerializerRegistry {
    private static final HashMap<Class<?>, Serializer<?, ? extends NbtElement>> registry = new HashMap<Class<?>, Serializer<?, ? extends NbtElement>>();

    private static class Serializer<T, E extends NbtElement> {
        private final Function<T, E> serializer;
        private final Function<E, T> deserializer;
    
        public Serializer(Function<T, E> serializer, Function<E, T> deserializer) {
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        @SuppressWarnings("unchecked")
        public NbtElement serialize(Object value) {
            return serializer.apply((T) value);
        }

        @SuppressWarnings("unchecked")
        public T deserialize(NbtElement value) {
            return deserializer.apply((E) value);
        }
    }

    static {
        registry.put(byte.class, new Serializer<Byte, NbtByte>((val) -> NbtByte.of(val), (el) -> el.byteValue()));
        registry.put(short.class, new Serializer<Short, NbtShort>((val) -> NbtShort.of(val), (el) -> el.shortValue()));
        registry.put(int.class, new Serializer<Integer, NbtInt>((val) -> NbtInt.of(val), (el) -> el.intValue()));
        registry.put(long.class, new Serializer<Long, NbtLong>((val) -> NbtLong.of(val), (el) -> el.longValue()));
        registry.put(float.class, new Serializer<Float, NbtFloat>((val) -> NbtFloat.of(val), (el) -> el.floatValue()));
        registry.put(double.class, new Serializer<Double, NbtDouble>((val) -> NbtDouble.of(val), (el) -> el.doubleValue()));
        registry.put(boolean.class, new Serializer<Boolean, NbtByte>((val) -> NbtByte.of(val), (el) -> el.byteValue() != 0));

        registry.put(byte[].class, new Serializer<Byte[], NbtByteArray>((val) -> new NbtByteArray(ArrayUtils.toPrimitive(val)), (el) -> ArrayUtils.toObject(el.getByteArray())));
        registry.put(int[].class, new Serializer<Integer[], NbtIntArray>((val) -> new NbtIntArray(ArrayUtils.toPrimitive(val)), (el) -> ArrayUtils.toObject(el.getIntArray())));
        registry.put(long[].class, new Serializer<Long[], NbtLongArray>((val) -> new NbtLongArray(ArrayUtils.toPrimitive(val)), (el) -> ArrayUtils.toObject(el.getLongArray())));

        registry.put(String.class, new Serializer<String, NbtString>((val) -> NbtString.of(val), (el) -> el.toString()));
        registry.put(UUID.class, new Serializer<UUID, NbtIntArray>((val) -> NbtHelper.fromUuid(val), (el) -> NbtHelper.toUuid(el)));

        registry.put(ChatMode.class, createEnumSez(ChatMode.class));
        registry.put(Rank.class, createEnumSez(Rank.class));
        registry.put(Status.class, createEnumSez(Status.class));
    }

    public static boolean contains(Class<?> clazz) {
        return registry.containsKey(clazz);
    }

    public static <T> NbtElement toNbtElement(Class<T> clazz, T value) {        
        return registry.get(clazz).serialize(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromNbtElement(Class<T> clazz, NbtElement value) {        
        return (T) registry.get(clazz).deserialize(value);
    }

    private static <T extends java.lang.Enum<T>> Serializer<T, NbtString> createEnumSez(Class<T> clazz) {
        return new Serializer<T, NbtString>(
            (val) -> NbtString.of(val.toString()),
            (el) -> Enum.valueOf(clazz, el.toString())
        );
    }
}