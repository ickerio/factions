package io.icker.factions.database;

import java.util.function.BiFunction;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.nbt.NbtCompound;

public class TypeSerializer<T> {
    private final TriConsumer<NbtCompound, String, T> writer;
    private final BiFunction<NbtCompound, String, T> reader;

    public TypeSerializer(TriConsumer<NbtCompound, String, T> writer, BiFunction<NbtCompound, String, T> reader) {
        this.writer = writer;
        this.reader = reader;
    }

    public T readNbt(String key, NbtCompound nbt) {
        if (!nbt.contains(key)) return null;
        return reader.apply(nbt, key);
    }

    public void writeNbt(String key, NbtCompound nbt, T value) {
        if (value != null) writer.accept(nbt, key, value);
    }

}
