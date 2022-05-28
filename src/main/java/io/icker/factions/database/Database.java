package io.icker.factions.database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import io.icker.factions.FactionsMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

public class Database {
    private static final File BASE_PATH = new File("factions");
    private static final HashMap<Class<?>, HashMap<String, Field>> cache = new HashMap<Class<?>, HashMap<String, Field>>();

    private static <T extends Persistent> void setup(Class<T> clazz) {
        String name = clazz.getAnnotation(Name.class).value();
        File file = new File(BASE_PATH, name.toLowerCase(Locale.ROOT)+".dat");

        if (!file.exists()) {
            try {
                NbtIo.writeCompressed(new NbtCompound(), file);
            } catch (IOException e) {
                FactionsMod.LOGGER.info("File creation failed", e);
            }
        }

        HashMap<String, Field> fields = new HashMap<String, Field>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(io.icker.factions.database.Field.class)) {
                fields.put(field.getAnnotation(io.icker.factions.database.Field.class).value(), field);
            }
        }

        cache.put(clazz, fields);
    }

    public static <T extends Persistent, E> HashMap<E, T> load(Class<T> clazz, Function<T, E> getStoreKey) {
        String name = clazz.getAnnotation(Name.class).value();
        if (!cache.containsKey(clazz)) setup(clazz);
        HashMap<String, Field> fields = cache.get(clazz);

        File file = new File(BASE_PATH, name.toLowerCase(Locale.ROOT) + ".dat");

        HashMap<E, T> store = new HashMap<E, T>();

        try {
            NbtCompound fileData = NbtIo.readCompressed(file);
            for (String id : fileData.getKeys()) {
                T item = (T) clazz.getDeclaredConstructor().newInstance();

                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Field field = entry.getValue();

                    Object element = TypeSerializerRegistry.get(field.getType()).readNbt(key, fileData.getCompound(id));

                    field.set(item, element);
                }

                store.put(getStoreKey.apply(item), item);
            }
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to read NBT data", e);
        }

        return store;
    }

    public static <T extends Persistent> void save(Class<T> clazz, List<T> items) {
        String name = clazz.getAnnotation(Name.class).value();
        HashMap<String, Field> fields = cache.get(clazz);

        File file = new File(BASE_PATH, name.toLowerCase(Locale.ROOT) + ".dat");

        NbtCompound fileData = new NbtCompound();

        try {
            for (T item : items) {
                NbtCompound compound = new NbtCompound();
                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Field field = entry.getValue();

                    Class<?> type = field.getType();
                    Object data = field.get(item);

                    TypeSerializer<?> serializer = TypeSerializerRegistry.get(type);
                    serializer.writeNbt(key, compound, parse(data));
                }
                fileData.put(item.getKey(), compound);
            }

            NbtIo.writeCompressed(fileData, file);
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to write NBT data", e);
        }
    }

    // TODO Safely remove this hacky cast
    private static <T> T parse(Object key) {
        return (T) key;
    }
}