package io.icker.factions.database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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

    private static <T extends Persistent> void setup(Class<T> clazz) {
        String name = clazz.getAnnotation(Name.class).value();

        if (!BASE_PATH.exists()) {
            BASE_PATH.mkdir();
        }

        File file = new File(BASE_PATH, name.toLowerCase(Locale.ROOT) + ".dat");

        if (!file.exists() && !clazz.getAnnotation(Name.class).child()) {
            try {
                NbtIo.writeCompressed(new NbtCompound(), file);
            } catch (IOException e) {
                FactionsMod.LOGGER.error("Failed to create NBT file", e);
            }
        }

        HashMap<String, Field> fields = new HashMap<String, Field>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(io.icker.factions.database.Field.class)) {
                fields.put(field.getAnnotation(io.icker.factions.database.Field.class).value(), field);
            } else if (field.isAnnotationPresent(Child.class)) {
                fields.put(field.getAnnotation(Child.class).value().getAnnotation(io.icker.factions.database.Name.class).value(), field);
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
                    NbtCompound itemData = fileData.getCompound(id);

                    if (field.isAnnotationPresent(io.icker.factions.database.Field.class)) {
                        if (itemData.contains(key) || !field.getAnnotation(io.icker.factions.database.Field.class).nullable()) {
                            Object element = TypeSerializerRegistry.get(field.getType()).readNbt(key, itemData);
                            field.set(item, element);
                        }
                    } else {
                        if (itemData.contains(key) || !field.getAnnotation(Child.class).nullable()) {
                            if (field.getAnnotation(Child.class).list()) {
                                field.set(item, loadList(field.getAnnotation(Child.class).value(), (NbtList) itemData.get(key)));
                            } else {
                                field.set(item, loadIndividual(field.getAnnotation(Child.class).value(), itemData.getCompound(key)));
                            }
                        }
                    }
                }

                store.put(getStoreKey.apply(item), item);
            }
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to read NBT data", e);
        }

        return store;
    }

    private static <T extends Persistent> T loadIndividual(Class<T> clazz, NbtCompound data) {
        if (!cache.containsKey(clazz)) setup(clazz);
        HashMap<String, Field> fields = cache.get(clazz);

        try {
            T item = (T) clazz.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                String key = entry.getKey();
                Field field = entry.getValue();

                if (data.contains(key) || !field.getAnnotation(io.icker.factions.database.Field.class).nullable()) {
                    Object element = TypeSerializerRegistry.get(field.getType()).readNbt(key, data);
                    field.set(item, element);
                }
            }

            return item;
        } catch (ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to read NBT data", e);
        }

        return null;
    }

    private static <T extends Persistent> ArrayList<T> loadList(Class<T> clazz, NbtList list) {
        if (!cache.containsKey(clazz)) setup(clazz);
        HashMap<String, Field> fields = cache.get(clazz);

        ArrayList<T> store = new ArrayList<>();

        try {
            for (int i = 0; i < list.size(); i++) {
                NbtCompound data = list.getCompound(i);
                T item = (T) clazz.getDeclaredConstructor().newInstance();

                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Field field = entry.getValue();

                    if (data.contains(key) || !field.getAnnotation(io.icker.factions.database.Field.class).nullable()) {
                        Object element = TypeSerializerRegistry.get(field.getType()).readNbt(key, data);
                        field.set(item, element);
                    }
                }

                store.add(item);
            }
        } catch (ReflectiveOperationException e) {
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

                    if (field.isAnnotationPresent(io.icker.factions.database.Field.class)) {
                        if (data != null || !field.getAnnotation(io.icker.factions.database.Field.class).nullable()) {
                            TypeSerializer<?> serializer = TypeSerializerRegistry.get(type);
                            serializer.writeNbt(key, compound, cast(data));
                        }
                    } else {
                        if (data != null || !field.getAnnotation(Child.class).nullable()) {
                            if (field.getAnnotation(Child.class).list()) {
                                compound.put(key, saveList(field.getAnnotation(Child.class).value(), cast(data)));
                            } else {
                                compound.put(key, saveIndividual(field.getAnnotation(Child.class).value(), cast(data)));
                            }
                        }
                    }
                }
                fileData.put(item.getKey(), compound);
            }

            NbtIo.writeCompressed(fileData, file);
        } catch (IOException | ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to write NBT data", e);
        }
    }

    private static <T extends Persistent> NbtCompound saveIndividual(Class<T> clazz, T item) {
        if (!cache.containsKey(clazz)) setup(clazz);
        HashMap<String, Field> fields = cache.get(clazz);

        try {
            NbtCompound compound = new NbtCompound();
            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                String key = entry.getKey();
                Field field = entry.getValue();

                Class<?> type = field.getType();
                Object data = field.get(item);

                if (data != null || !field.getAnnotation(io.icker.factions.database.Field.class).nullable()) {
                    TypeSerializer<?> serializer = TypeSerializerRegistry.get(type);
                    serializer.writeNbt(key, compound, cast(data));
                }
            }

            return compound;
        } catch (ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to write NBT data", e);
        }

        return null;
    }

    private static <T extends Persistent> NbtList saveList(Class<T> clazz, ArrayList<T> items) {
        if (!cache.containsKey(clazz)) setup(clazz);
        HashMap<String, Field> fields = cache.get(clazz);

        NbtList list = new NbtList();

        try {
            for (T item : items) {
                NbtCompound compound = new NbtCompound();
                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Field field = entry.getValue();

                    Class<?> type = field.getType();
                    Object data = field.get(item);

                    if (data != null || !field.getAnnotation(io.icker.factions.database.Field.class).nullable()) {
                        TypeSerializer<?> serializer = TypeSerializerRegistry.get(type);
                        serializer.writeNbt(key, compound, cast(data));
                    }
                }

                list.add(list.size(), compound);
            }
        } catch (ReflectiveOperationException e) {
            FactionsMod.LOGGER.error("Failed to write NBT data", e);
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object key) {
        return (T) key;
    }
}