package io.icker.factions.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;

public class Translator {
    private static final HashMap<String, HashMap<String, String>> CACHE = new HashMap<>();
    private static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("factions");

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
