package io.icker.factions.database2;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.icker.factions.FactionsMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

public class Database {
    public <T> List<T> loadCompound(Class<T> clazz) {
        String name = clazz.getAnnotation(Compound.class).value();

        HashMap<String, Field> fields = new HashMap<String, Field>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Element.class)) {
                fields.put(field.getAnnotation(Element.class).value(), field);
            }
        }

        File directory = new File("factions/" + name);
        String paths[] = directory.list();

        ArrayList<T> items = new ArrayList<T>();

        for (String path : paths) {
            try {
                NbtCompound compound = NbtIo.readCompressed(new File(path));

                T item = clazz.getDeclaredConstructor().newInstance();

                for (String key : compound.getKeys()) {
                    // TODO at the moment can only read strings
                    fields.get(key).set(item, compound.getString(key));
                }
            } catch (IOException | ReflectiveOperationException e) {
                FactionsMod.LOGGER.error("Failed to read NBT data");
            }
        }

        return items;
    }
}
