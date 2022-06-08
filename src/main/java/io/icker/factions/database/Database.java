package io.icker.factions.database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;

import io.icker.factions.FactionsMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

public class Database {
    private static final File BASE_PATH = FabricLoader.getInstance().getGameDir().resolve("factions").toFile();
    private static final HashMap<Class<?>, HashMap<String, Field>> cache = new HashMap<Class<?>, HashMap<String, Field>>();

    private static <T> void setup(Class<T> clazz) {
        HashMap<String, Field> fields = new HashMap<String, Field>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(io.icker.factions.database.Field.class)) {
                field.setAccessible(true);
                fields.put(field.getAnnotation(io.icker.factions.database.Field.class).value(), field);

                // Setup children
                Class<?> type = field.getType();
                if (!TypeSerializerRegistry.contains(type)) {
                    if (Collection.class.isAssignableFrom(type)) {
                        ParameterizedType pType = (ParameterizedType) field.getGenericType();
                        setup((Class<?>) pType.getActualTypeArguments()[0]);
                    } else {
                        setup(type);
                    }
                }
            }
        }

        cache.put(clazz, fields);
    }

    public static <T extends Persistent, E> HashMap<E, T> load(Class<T> clazz, Function<T, E> getStoreKey) {
        String name = clazz.getAnnotation(Name.class).value();
        File file = new File(BASE_PATH, name.toLowerCase(Locale.ROOT) + ".dat");

        if (!cache.containsKey(clazz)) setup(clazz);

        HashMap<E, T> store = new HashMap<E, T>();

        if (!file.exists()) {
            return store;
        }

        try {
            NbtCompound fileData = NbtIo.readCompressed(file);
            for (T item : deserializeList(clazz, (NbtList) fileData.get("CORE"))) {
                store.put(getStoreKey.apply(item), item);
            }
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to read NBT data ({})", file, e);
        }

        return store;
    }

    private static <T> T deserialize(Class<T> clazz, NbtCompound data) throws IOException, ReflectiveOperationException {
        HashMap<String, Field> fields = cache.get(clazz);

        T item = (T) clazz.getDeclaredConstructor().newInstance();

        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String key = entry.getKey();
            Field field = entry.getValue();

            if (!data.contains(key)) continue;

            Class<?> type = field.getType();
            if (TypeSerializerRegistry.contains(type)) {
                // Standard SerializedType
                Object element = TypeSerializerRegistry.get(type).readNbt(key, data);
                field.set(item, element);
            } else {
                if (Collection.class.isAssignableFrom(type)) {
                    // ArrayList
                    ParameterizedType pType = (ParameterizedType) field.getGenericType();
                    field.set(item, deserializeList((Class<?>) pType.getActualTypeArguments()[0], (NbtList) data.get(key)));
                } else {
                    // Embed
                    field.set(item, deserialize(type, data.getCompound(key)));
                }
            }
        }

        return item;
    }

    private static <T> ArrayList<T> deserializeList(Class<T> clazz, NbtList list) throws IOException, ReflectiveOperationException {
        ArrayList<T> store = new ArrayList<T>();

        for (int i = 0; i < list.size(); i++) {
            NbtCompound data = list.getCompound(i);
            store.add(deserialize(clazz, data));
        }

        return store;
    }

    public static <T extends Persistent> void save(Class<T> clazz, List<T> items) {
        String name = clazz.getAnnotation(Name.class).value();
        File file = new File(BASE_PATH, name.toLowerCase(Locale.ROOT) + ".dat");

        if (!cache.containsKey(clazz)) setup(clazz);

        try {
            NbtCompound fileData = new NbtCompound();
            fileData.put("CORE",  serializeList(clazz, items));
            NbtIo.writeCompressed(fileData, file);
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to write NBT data ({})", file, e);
        }
    }

    private static <T> NbtCompound serialize(Class<T> clazz, T item) throws IOException, ReflectiveOperationException {
        HashMap<String, Field> fields = cache.get(clazz);

        NbtCompound compound = new NbtCompound();
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String key = entry.getKey();
            Field field = entry.getValue();

            Class<?> type = field.getType();
            Object data = field.get(item);

            if (data == null) continue;

            if (TypeSerializerRegistry.contains(type)) {
                TypeSerializer<?> serializer = TypeSerializerRegistry.get(type);
                serializer.writeNbt(key, compound, cast(data));
            } else {
                if (Collection.class.isAssignableFrom(type)) {
                    ParameterizedType pType = (ParameterizedType) field.getGenericType();
                    compound.put(key, serializeList((Class<?>) pType.getActualTypeArguments()[0], cast(data)));
                } else {
                    compound.put(key, serialize(type, cast(data)));
                }
            }
        }

        return compound;
    }

    private static <T> NbtList serializeList(Class<T> clazz, Collection<T> items) throws IOException, ReflectiveOperationException {
        NbtList list = new NbtList();

        for (T item : items) {
            list.add(list.size(), serialize(clazz, item));
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object key) {
        return (T) key;
    }
}