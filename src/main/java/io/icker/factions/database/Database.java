package io.icker.factions.database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import io.icker.factions.FactionsMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.apache.commons.io.FilenameUtils;

public class Database {
    private static final File BASE_PATH = new File("factions");
    private static final HashMap<Class<?>, HashMap<String, Field>> cache = new HashMap<Class<?>, HashMap<String, Field>>();

    private static <T extends Persistent> void setup(Class<T> clazz) {
        String name = clazz.getAnnotation(Name.class).value();
        File directory = new File(BASE_PATH, name);

        if (directory.isFile()) {
            FactionsMod.LOGGER.error("File already exists in database directory path " + directory.toString());
            return;
        }

        directory.mkdirs();

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

        File directory = new File(BASE_PATH, name);
        File files[] = directory.listFiles();

        HashMap<E, T> store = new HashMap<E, T>();

        for (File file : files) {
            try {
                NbtCompound compound = NbtIo.readCompressed(file);
                T item = (T) clazz.getDeclaredConstructor().newInstance();

                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Field field = entry.getValue();

                    Object element = TypeSerializerRegistry.get(field.getType()).readNbt(key, compound);

                    field.set(item, element);
                }

                store.put(getStoreKey.apply(item), item);

            } catch (IOException | ReflectiveOperationException e) {
                FactionsMod.LOGGER.error("Failed to read NBT data", e);
            }
        }

        FactionsMod.LOGGER.info(store.toString());

        return store;
    }

    public static <T extends Persistent> void save(Class<T> clazz, List<T> items) {
        String name = clazz.getAnnotation(Name.class).value();
        HashMap<String, Field> fields = cache.get(clazz);

        File path = new File(BASE_PATH, name);

        for (T item : items) {
            NbtCompound compound = new NbtCompound();

            try {
                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Field field = entry.getValue();

                    Class<?> type = field.getType();
                    Object data = field.get(item);

                    TypeSerializer<?> serializer = TypeSerializerRegistry.get(type);
                    serializer.writeNbt(key, compound, parse(data));
                }

                File file = new File(path, item.getKey() + ".dat");
                NbtIo.writeCompressed(compound, file);

            } catch (IOException | ReflectiveOperationException e ) {
                FactionsMod.LOGGER.error("Failed to write NBT data", e);
            }
        }
    }

    // TODO Safely remove this hacky cast
    private static <T> T parse(Object key) {
        return (T) key;
    }
}