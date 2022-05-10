package io.icker.factions.database2;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.icker.factions.FactionsMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

public class Database {
    public static final File BASE_PATH = new File("factions");
    public static final HashMap<Class<?>, HashMap<String, Field>> cache = new HashMap<Class<?>, HashMap<String, Field>>();

    public static <T extends Persistent> void setup(Class<T> clazz) {
        String name = clazz.getAnnotation(Persistent.Name.class).value();
        File directory = new File(BASE_PATH + name);

        if (directory.isFile()) {
            FactionsMod.LOGGER.error("File already exists in database directory path " + directory.toString());
            return;
        }

        directory.mkdirs();

        HashMap<String, Field> fields = new HashMap<String, Field>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Persistent.Field.class)) {
                fields.put(field.getAnnotation(Persistent.Field.class).value(), field);
            }
        }

        cache.put(clazz, fields);
    }

    public static <T extends Persistent> List<T> load(Class<T> clazz) {
        String name = clazz.getAnnotation(Persistent.Name.class).value();
        HashMap<String, Field> fields = cache.get(clazz);

        File directory = new File(BASE_PATH, name);
        File files[] = directory.listFiles();

        ArrayList<T> items = new ArrayList<T>();

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

                items.add(item);

            } catch (IOException | ReflectiveOperationException e) {
                FactionsMod.LOGGER.error("Failed to read NBT data");
            }
        }

        return items;
    }

    public static <T extends Persistent> void save(Class<T> clazz, List<T> items) {
        String name = clazz.getAnnotation(Persistent.Name.class).value();
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

                    write(type, data, key, compound);
                }

                File file = new File(path, item.getKey() + ".dat");
                NbtIo.writeCompressed(compound, file);

            } catch(IOException | ReflectiveOperationException e ) {
                FactionsMod.LOGGER.error("Failed to write NBT data");
            }
        }
    }

    // TODO avoid this new function
    private static <T> void write(Class<T> type, Object data, String key, NbtCompound compound) {
        TypeSerializer<T> serializer = TypeSerializerRegistry.get(type);
        serializer.writeNbt(key, compound, type.cast(data));
    }
}