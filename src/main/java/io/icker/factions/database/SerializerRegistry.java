package io.icker.factions.database;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.Relationship.Status;
import io.icker.factions.api.persistents.User.ChatMode;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.api.persistents.User.SoundMode;
import io.icker.factions.util.WorldUtils;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueInput.TypedInputList;
import net.minecraft.world.level.storage.ValueOutput.TypedOutputList;

import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

public class SerializerRegistry {
    private static final HashMap<Class<?>, Serializer<?, ? extends Tag>> registry =
            new HashMap<Class<?>, Serializer<?, ? extends Tag>>();

    private static class Serializer<T, E extends Tag> {
        private final Function<T, E> serializer;
        private final Function<E, T> deserializer;

        public Serializer(Function<T, E> serializer, Function<E, T> deserializer) {
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        @SuppressWarnings("unchecked")
        public Tag serialize(Object value) {
            return serializer.apply((T) value);
        }

        @SuppressWarnings("unchecked")
        public T deserialize(Tag value) {
            return deserializer.apply((E) value);
        }
    }

    static {
        registry.put(
                byte.class,
                new Serializer<Byte, ByteTag>(val -> ByteTag.valueOf(val), el -> el.byteValue()));
        registry.put(
                short.class,
                new Serializer<Short, ShortTag>(
                        val -> ShortTag.valueOf(val), el -> el.shortValue()));
        registry.put(
                int.class,
                new Serializer<Integer, IntTag>(val -> IntTag.valueOf(val), el -> el.intValue()));
        registry.put(
                long.class,
                new Serializer<Long, LongTag>(val -> LongTag.valueOf(val), el -> el.longValue()));
        registry.put(
                float.class,
                new Serializer<Float, FloatTag>(
                        val -> FloatTag.valueOf(val), el -> el.floatValue()));
        registry.put(
                double.class,
                new Serializer<Double, DoubleTag>(
                        val -> DoubleTag.valueOf(val), el -> el.doubleValue()));
        registry.put(
                boolean.class,
                new Serializer<Boolean, ByteTag>(
                        val -> ByteTag.valueOf(val), el -> el.byteValue() != 0));

        registry.put(
                byte[].class,
                new Serializer<Byte[], ByteArrayTag>(
                        val -> new ByteArrayTag(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getAsByteArray())));
        registry.put(
                int[].class,
                new Serializer<Integer[], IntArrayTag>(
                        val -> new IntArrayTag(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getAsIntArray())));
        registry.put(
                long[].class,
                new Serializer<Long[], LongArrayTag>(
                        val -> new LongArrayTag(ArrayUtils.toPrimitive(val)),
                        el -> ArrayUtils.toObject(el.getAsLongArray())));

        registry.put(
                String.class,
                new Serializer<String, StringTag>(
                        val -> StringTag.valueOf(val), el -> el.asString().orElse("")));
        registry.put(
                UUID.class,
                new Serializer<UUID, IntArrayTag>(
                        val -> new IntArrayTag(UUIDUtil.uuidToIntArray(val)),
                        el -> UUIDUtil.uuidFromIntArray(el.getAsIntArray())));
        registry.put(SimpleContainer.class, createInventorySerializer(54));

        registry.put(ChatMode.class, createEnumSerializer(ChatMode.class));
        registry.put(SoundMode.class, createEnumSerializer(SoundMode.class));
        registry.put(Rank.class, createEnumSerializer(Rank.class));
        registry.put(Status.class, createEnumSerializer(Status.class));
        registry.put(Permissions.class, createEnumSerializer(Permissions.class));
    }

    public static boolean contains(Class<?> clazz) {
        return registry.containsKey(clazz);
    }

    public static <T> Tag toNbtElement(Class<T> clazz, T value) {
        return registry.get(clazz).serialize(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromNbtElement(Class<T> clazz, Tag value) {
        return (T) registry.get(clazz).deserialize(value);
    }

    private static <T extends Enum<T>> Serializer<T, StringTag> createEnumSerializer(
            Class<T> clazz) {
        return new Serializer<T, StringTag>(
                val -> StringTag.valueOf(val.toString()),
                el -> Enum.valueOf(clazz, el.asString().orElse("")));
    }

    public record InventoryItem(int slot, ItemStack stack) {
        public static final Codec<InventoryItem> CODEC =
                RecordCodecBuilder.create(
                        (instance) -> {
                            return instance.group(
                                            ExtraCodecs.UNSIGNED_BYTE
                                                    .fieldOf("Slot")
                                                    .orElse(0)
                                                    .forGetter(InventoryItem::slot),
                                            ItemStack.MAP_CODEC
                                                    .fieldOf("Data")
                                                    .forGetter(InventoryItem::stack))
                                    .apply(instance, InventoryItem::new);
                        });
    }

    private static Serializer<SimpleContainer, ListTag> createInventorySerializer(int size) {
        return new Serializer<SimpleContainer, ListTag>(
                val -> {
                    ProblemReporter.ScopedCollector reporter =
                            new ProblemReporter.ScopedCollector(FactionsMod.LOGGER);
                    TagValueOutput view =
                            TagValueOutput.createWithContext(
                                    reporter,
                                    WorldUtils.getWorld("minecraft:overworld").registryAccess());
                    TypedOutputList<InventoryItem> appender =
                            view.list("Data", InventoryItem.CODEC);

                    for (int i = 0; i < val.getContainerSize(); ++i) {
                        ItemStack itemStack = val.getItem(i);
                        if (!itemStack.isEmpty()) {
                            appender.add(new InventoryItem(i, itemStack));
                        }
                    }

                    reporter.close();

                    return view.buildResult().getList("Data").get();
                },
                el -> {
                    CompoundTag compound = new CompoundTag();
                    compound.put("Data", el);

                    ProblemReporter.ScopedCollector reporter =
                            new ProblemReporter.ScopedCollector(FactionsMod.LOGGER);

                    ValueInput view =
                            TagValueInput.create(
                                    reporter,
                                    WorldUtils.getWorld("minecraft:overworld").registryAccess(),
                                    compound);

                    SimpleContainer inventory = new SimpleContainer(size);

                    for (int i = 0; i < size; ++i) {
                        inventory.setItem(i, ItemStack.EMPTY);
                    }

                    TypedInputList<InventoryItem> list_view =
                            view.listOrEmpty("Data", InventoryItem.CODEC);

                    for (InventoryItem item : list_view) {
                        inventory.setItem(item.slot(), item.stack());
                    }

                    reporter.close();

                    return inventory;
                });
    }
}
