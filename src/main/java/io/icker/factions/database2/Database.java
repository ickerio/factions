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
    public static <T> List<T> load(Class<T> clazz) {
        String name = clazz.getAnnotation(Compound.class).value();

        HashMap<String, Field> fields = new HashMap<String, Field>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Element.class)) {
                fields.put(field.getAnnotation(Element.class).value(), field);
            }
        }

        File directory = new File("factions/" + name); // TODO check if its a file or if its empty and skip
        File files[] = directory.listFiles();

        ArrayList<T> items = new ArrayList<T>();

        for (File file : files) {
            try {
                NbtCompound compound = NbtIo.read(file);
                T item = (T) clazz.getDeclaredConstructor().newInstance();

                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Field field = entry.getValue();
                    
                    Object element = TypeSerializerRegistry.get(field.getType()).readNbt(key, compound);

                    field.set(item, element);
                }

                items.add(item);

            } catch (IOException | ReflectiveOperationException e) {
                FactionsMod.LOGGER.error(e);
                FactionsMod.LOGGER.error("Failed to read NBT data");
            }
        }

        return items;
    }

    public static <T> void save(Class<T> clazz, List<T> items) {
        String name = clazz.getAnnotation(Compound.class).value();

        HashMap<String, Field> fields = new HashMap<String, Field>();
        
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Element.class)) {
                fields.put(field.getAnnotation(Element.class).value(), field);
            }
        }

        for (T item : items) {
            NbtCompound compound = new NbtCompound();

            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                String key = entry.getKey();
                Field field = entry.getValue();

                try {
                    Class<?> type = field.getType();
                    Object data = field.get(item);
    
                    //TypeSerializer<?> serializer = TypeSerializerRegistry.get(type);
                    //serializer.writeNbt(key, compound, type.cast(data));
    
                    write(type, data, key, compound);
                } catch(ReflectiveOperationException e ) {
                    FactionsMod.LOGGER.error("Failed to write NBT data");
                }
            }

            // TODO multiple files by ID && fix this
            File file = new File("factions/" + name + "/1.dat");
            try {
                NbtIo.write(compound, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO avoid this new function
    private static <T> void write(Class<T> type, Object data, String key, NbtCompound compound) {
        TypeSerializer<T> serializer = TypeSerializerRegistry.get(type);
        serializer.writeNbt(key, compound, type.cast(data));
    }
}