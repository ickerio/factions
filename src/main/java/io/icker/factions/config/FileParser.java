package io.icker.factions.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.icker.factions.FactionsMod;

public final class FileParser extends Parser {
    protected File configFile;

    protected static File getFile(String[] filePaths) throws IOException {
        for (String path : filePaths) {
            File file = new File(path);

            if (file.exists()) {
                return file;
            }
        }

        File file = new File(filePaths[0]);
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }

    public FileParser(Integer version, String[] filePaths) throws IOException {
        this.configFile = getFile(filePaths);
        FileReader reader = new FileReader(this.configFile);
        this.object = GSON.fromJson(reader, JsonObject.class);
        if (this.object == null) {
            this.object = new JsonObject();
        }
        reader.close();

        if (this.getInteger("version", version) != version) {
            FactionsMod.LOGGER.error(String.format("Config file incompatible (requires version %d)", version));
        }
    }

    protected void update() throws IOException {
        FileWriter writer = new FileWriter(this.configFile);
        writer.write(GSON.toJson(this.object));
        writer.close();
    }

    protected void create(String key, JsonElement fallback) throws IOException {
        super.create(key, fallback);
        update();
    }
}