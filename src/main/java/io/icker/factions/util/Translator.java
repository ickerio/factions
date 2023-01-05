package io.icker.factions.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.icker.factions.FactionsMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Translator {
    private static final HashMap<String, HashMap<String, String>> CACHE = new HashMap<>();
    private static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("factions");

    public static final SimpleSynchronousResourceReloadListener listener = new SimpleSynchronousResourceReloadListener() {
        @Override
        public Identifier getFabricId() {
            return new Identifier(FactionsMod.MODID, "languages");
        }

        @Override
        public void reload(ResourceManager manager) {
            for(Map.Entry<Identifier, Resource> entry : manager.findResources("lang", path -> path.getPath().endsWith(".json")).entrySet()) {
                Gson gson = new GsonBuilder().create();
                try {
                    CACHE.put(FilenameUtils.getBaseName(entry.getKey().getPath()), gson.fromJson(entry.getValue().getReader(), new TypeToken<HashMap<String, String>>(){}.getType()));
                } catch (IOException e) {
                    FactionsMod.LOGGER.error("Could not load lang file", e);
                }
            }
        }
    };

    public static String get(String key, String lang) {
        if (!CACHE.containsKey(lang)) {
            loadFile(lang);
        }

        if (CACHE.get(lang).containsKey(key)) {
            return CACHE.get(lang).get(key);
        }

        return key;
    }

    private static void loadFile(String lang) {
        File file = DIR.resolve(lang+".json").toFile();

        if (!file.exists()) {
            CACHE.put(lang, new HashMap<>());
        } else {
            Gson gson = new GsonBuilder().create();

            try {
                CACHE.put(lang, gson.fromJson(new FileReader(file), new TypeToken<HashMap<String, String>>(){}.getType()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
