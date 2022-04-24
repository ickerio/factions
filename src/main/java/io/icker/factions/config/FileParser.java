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

    protected static File getFile(String fileName, String[] directories) throws IOException {
        for (String dir : directories) {
            File file = new File(dir, fileName);

            if (file.exists()) {
                return file;
            }
        }

        File dir = new File(directories[0] + "/");
        dir.mkdir();
        File file = new File(dir, fileName);
        file.createNewFile();
        return file;
    }

    public FileParser(Integer version, String fileName, String[] directories) throws IOException {
        this.configFile = getFile(fileName, directories);
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

    public FileParser(int version, String fileName) throws IOException {
        this(version, fileName, new String[]{"config"});
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