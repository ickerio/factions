package io.icker.factions.database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import io.icker.factions.FactionsMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

public class Database {
    private static final File BASE_PATH =
            FabricLoader.getInstance().getGameDir().resolve("factions").toFile();
    private static final HashMap<Class<?>, HashMap<String, Field>> cache =
            new HashMap<Class<?>, HashMap<String, Field>>();
    private static final String KEY = "CORE";

    public static <T, E> HashMap<E, T> load(Class<T> clazz, Function<T, E> getStoreKey) {
        String name = clazz.getAnnotation(Name.class).value();
        File file = new File(BASE_PATH, name.toLowerCase() + ".dat");

        if (!cache.containsKey(clazz))
            setup(clazz);

        HashMap<E, T> store = new HashMap<E, T>();

        if (!file.exists()) {
            if (!BASE_PATH.exists())
                BASE_PATH.mkdir();
            try {
                file.createNewFile();
            } catch (IOException e) {
                FactionsMod.LOGGER.error("Failed to create file ({})", file, e);
            }
            return store;
        }

        try {
            NbtList list = (NbtList) NbtIo.readCompressed(file).get(KEY);
            for (T item : deserializeList(clazz, list)) {
                store.put(getStoreKey.apply(item), item);
            }
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to read NBT data ({})", file, e);
        }

        return store;
    }

    private static <T> T deserialize(Class<T> clazz, NbtElement value)
            throws IOException, ReflectiveOperationException {
        if (SerializerRegistry.contains(clazz)) {
            return SerializerRegistry.fromNbtElement(clazz, value);
        }

        NbtCompound compound = (NbtCompound) value;
        T item = (T) clazz.getDeclaredConstructor().newInstance();

        HashMap<String, Field> fields = cache.get(clazz);
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String key = entry.getKey();
            Field field = entry.getValue();

            if (!compound.contains(key))
                continue;

            Class<?> type = field.getType();

            if (ArrayList.class.isAssignableFrom(type)) {
                Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType())
                        .getActualTypeArguments()[0];
                field.set(item, deserializeList(genericType, (NbtList) compound.get(key)));
            } else {
                field.set(item, deserialize(type, compound.get(key)));
            }
        }

        return item;
    }

    private static <T> ArrayList<T> deserializeList(Class<T> clazz, NbtList list)
            throws IOException, ReflectiveOperationException {
        ArrayList<T> store = new ArrayList<T>();

        for (int i = 0; i < list.size(); i++) {
            store.add(deserialize(clazz, list.get(i)));
        }

        return store;
    }

    public static <T> void save(Class<T> clazz, List<T> items) {
        String name = clazz.getAnnotation(Name.class).value();
        File file = new File(BASE_PATH, name.toLowerCase() + ".dat");

        if (!cache.containsKey(clazz))
            setup(clazz);

        try {
            NbtCompound fileData = new NbtCompound();
            fileData.put(KEY, serializeList(clazz, items));
            NbtIo.writeCompressed(fileData, file);
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to write NBT data ({})", file, e);
        }
    }

    private static <T> NbtElement serialize(Class<T> clazz, T item)
            throws IOException, ReflectiveOperationException {
        if (SerializerRegistry.contains(clazz)) {
            return SerializerRegistry.toNbtElement(clazz, item);
        }

        HashMap<String, Field> fields = cache.get(clazz);
        NbtCompound compound = new NbtCompound();
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String key = entry.getKey();
            Field field = entry.getValue();

            Class<?> type = field.getType();
            Object data = field.get(item);

            if (data == null)
                continue;

            if (ArrayList.class.isAssignableFrom(type)) {
                Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType())
                        .getActualTypeArguments()[0];
                compound.put(key, serializeList(genericType, cast(data)));
            } else {
                compound.put(key, serialize(type, cast(data)));
            }
        }

        return compound;
    }

    private static <T> NbtList serializeList(Class<T> clazz, List<T> items)
            throws IOException, ReflectiveOperationException {
        NbtList list = new NbtList();

        for (T item : items) {
            list.add(list.size(), serialize(clazz, item));
        }

        return list;
    }

    private static <T> void setup(Class<T> clazz) {
        HashMap<String, Field> fields = new HashMap<String, Field>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(io.icker.factions.database.Field.class)) {
                field.setAccessible(true);
                fields.put(field.getAnnotation(io.icker.factions.database.Field.class).value(),
                        field);

                Class<?> type = field.getType();
                if (!SerializerRegistry.contains(type)) {
                    if (ArrayList.class.isAssignableFrom(type)) {
                        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                        setup((Class<?>) genericType.getActualTypeArguments()[0]);
                    } else {
                        setup(type);
                    }
                }
            }
        }

        cache.put(clazz, fields);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object key) {
        return (T) key;
    }
}
