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
import net.minecraft.registry.DynamicRegistryManager;

public class SerializerRegistry {
    private static final HashMap<Class<?>, Serializer<?, ? extends NbtElement>> registry = new HashMap<>();

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

    public static void initialize() {
        registry.put(byte.class,
                new Serializer<>(NbtByte::of, NbtByte::byteValue));
        registry.put(short.class,
                new Serializer<>(NbtShort::of, NbtShort::shortValue));
        registry.put(int.class,
                new Serializer<>(NbtInt::of, NbtInt::intValue));
        registry.put(long.class,
                new Serializer<>(NbtLong::of, NbtLong::longValue));
        registry.put(float.class,
                new Serializer<>(NbtFloat::of, NbtFloat::floatValue));
        registry.put(double.class,
                new Serializer<>(NbtDouble::of, NbtDouble::doubleValue));
        registry.put(boolean.class,
                new Serializer<>(val -> NbtByte.of((byte) (val ? 1 : 0)), el -> el.byteValue() != 0));

        registry.put(byte[].class,
                new Serializer<>(val -> new NbtByteArray(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getByteArray())));
        registry.put(int[].class,
                new Serializer<>(val -> new NbtIntArray(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getIntArray())));
        registry.put(long[].class,
                new Serializer<>(val -> new NbtLongArray(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getLongArray())));

        registry.put(String.class,
                new Serializer<>(NbtString::of, NbtString::asString));
        registry.put(UUID.class, new Serializer<>(NbtHelper::fromUuid,
                NbtHelper::toUuid));
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
        return new Serializer<>(val -> NbtString.of(val.toString()),
                el -> Enum.valueOf(clazz, el.asString()));
    }

    private static Serializer<SimpleInventory, NbtList> createInventorySerializer(int size) {
        return new Serializer<>(val -> {
            NbtList nbtList = new NbtList();

            for (int i = 0; i < val.size(); ++i) {
                ItemStack itemStack = val.getStack(i);
                if (!itemStack.isEmpty()) {
                    NbtCompound nbtCompound = new NbtCompound();
                    nbtCompound.putByte("Slot", (byte) i);
                    nbtCompound.put("Data", itemStack.writeNbt(new NbtCompound()));
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
                    inventory.setStack(slot, ItemStack.fromNbt(nbtCompound.getCompound("Data")));
                }
            }

            return inventory;
        });
    }
}
