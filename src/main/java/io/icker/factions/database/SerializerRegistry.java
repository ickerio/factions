package io.icker.factions.database;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import org.apache.commons.lang3.ArrayUtils;
import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.Relationship.Status;
import io.icker.factions.api.persistents.User.ChatMode;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.api.persistents.User.SoundMode;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;

public class SerializerRegistry {
    private static final HashMap<Class<?>, Serializer<?, ? extends NbtElement>> registry =
            new HashMap<Class<?>, Serializer<?, ? extends NbtElement>>();

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
        registry.put(byte.class,
                new Serializer<Byte, NbtByte>(val -> NbtByte.of(val), el -> el.byteValue()));
        registry.put(short.class,
                new Serializer<Short, NbtShort>(val -> NbtShort.of(val), el -> el.shortValue()));
        registry.put(int.class,
                new Serializer<Integer, NbtInt>(val -> NbtInt.of(val), el -> el.intValue()));
        registry.put(long.class,
                new Serializer<Long, NbtLong>(val -> NbtLong.of(val), el -> el.longValue()));
        registry.put(float.class,
                new Serializer<Float, NbtFloat>(val -> NbtFloat.of(val), el -> el.floatValue()));
        registry.put(double.class, new Serializer<Double, NbtDouble>(val -> NbtDouble.of(val),
                el -> el.doubleValue()));
        registry.put(boolean.class, new Serializer<Boolean, NbtByte>(val -> NbtByte.of(val),
                el -> el.byteValue() != 0));

        registry.put(byte[].class,
                new Serializer<Byte[], NbtByteArray>(
                        val -> new NbtByteArray(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getByteArray())));
        registry.put(int[].class,
                new Serializer<Integer[], NbtIntArray>(
                        val -> new NbtIntArray(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getIntArray())));
        registry.put(long[].class,
                new Serializer<Long[], NbtLongArray>(
                        val -> new NbtLongArray(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getLongArray())));

        registry.put(String.class,
                new Serializer<String, NbtString>(val -> NbtString.of(val), el -> el.asString()));
        registry.put(UUID.class, new Serializer<UUID, NbtIntArray>(val -> NbtHelper.fromUuid(val),
                el -> NbtHelper.toUuid(el)));
        registry.put(SimpleInventory.class, createInventorySerializer(54));

        registry.put(ChatMode.class, createEnumSerializer(ChatMode.class));
        registry.put(SoundMode.class, createEnumSerializer(SoundMode.class));
        registry.put(Rank.class, createEnumSerializer(Rank.class));
        registry.put(Status.class, createEnumSerializer(Status.class));
        registry.put(Permissions.class, createEnumSerializer(Permissions.class));
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

    private static <T extends Enum<T>> Serializer<T, NbtString> createEnumSerializer(
            Class<T> clazz) {
        return new Serializer<T, NbtString>(val -> NbtString.of(val.toString()),
                el -> Enum.valueOf(clazz, el.asString()));
    }

    private static Serializer<SimpleInventory, NbtList> createInventorySerializer(int size) {
        return new Serializer<SimpleInventory, NbtList>(val -> {
            NbtList nbtList = new NbtList();

            for (int i = 0; i < val.size(); ++i) {
                ItemStack itemStack = val.getStack(i);
                if (!itemStack.isEmpty()) {
                    NbtCompound nbtCompound = new NbtCompound();
                    nbtCompound.putByte("Slot", (byte) i);
                    itemStack.writeNbt(nbtCompound);
                    nbtList.add(nbtCompound);
                }
            }

            return nbtList;
        }, el -> {
            SimpleInventory inventory = new SimpleInventory(size);

            for (int i = 0; i < size; ++i) {
                inventory.setStack(i, ItemStack.EMPTY);
            }

            for (int i = 0; i < el.size(); ++i) {
                NbtCompound nbtCompound = el.getCompound(i);
                int slot = nbtCompound.getByte("Slot") & 255;
                if (slot >= 0 && slot < size) {
                    inventory.setStack(slot, ItemStack.fromNbt(nbtCompound));
                }
            }

            return inventory;
        });
    }
}
