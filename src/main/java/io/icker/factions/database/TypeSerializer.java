package io.icker.factions.database;

import java.util.function.BiFunction;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.nbt.NbtCompound;

public class TypeSerializer<T> {
    private final TriConsumer<NbtCompound, String, T> writer;
    private final BiFunction<NbtCompound, String, T> reader;
    private final T fallback;

    public TypeSerializer(TriConsumer<NbtCompound, String, T> writer, BiFunction<NbtCompound, String, T> reader, T fallback) {
        this.writer = writer;
        this.reader = reader;
        this.fallback = fallback;
    }

    public T readNbt(String key, NbtCompound nbt) {
        if (!nbt.contains(key)) return fallback;
        return reader.apply(nbt, key);
    }

    public void writeNbt(String key, NbtCompound nbt, T value) {
        writer.accept(nbt, key, value == null ? fallback : value);
    }

}
