package lycrex.sinfo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lycrex.sinfo.SInfoConstants;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.utils.PasswordUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "sinfo.json");
    private static ModConfig instance;

    public String apiPassword = SInfoConstants.DEFAULT_PASSWORD;
    public int apiPort = SInfoConstants.DEFAULT_API_PORT;
    public boolean downloadVanillaTextures = true;

    public String getPasswordHash() {
        return PasswordUtils.hashPassword(apiPassword);
    }

    public static ModConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                SInfoMod.LOGGER.error("Failed to load config", e);
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            SInfoMod.LOGGER.error("Failed to save config", e);
        }
    }
}

