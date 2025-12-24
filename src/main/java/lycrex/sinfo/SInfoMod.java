package lycrex.sinfo;

import lycrex.sinfo.api.ApiManager;
import lycrex.sinfo.api.event.SInfoEventListener;
import lycrex.sinfo.api.resource.VanillaResourceManager;
import lycrex.sinfo.command.SInfoCommandManager;
import lycrex.sinfo.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SInfoMod implements ModInitializer {
    public static final String MOD_ID = SInfoConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MinecraftServer serverInstance;

    public static MinecraftServer getServerInstance() {
        return serverInstance;
    }

    @Override
    public void onInitialize() {
        String mcVersion = net.minecraft.SharedConstants.getGameVersion().getName();
        LOGGER.info("SInfo Mod loading (MC: {})...", mcVersion);

        // Load config
        ModConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
            
            // Initialize resource manager (start pre-download)
            VanillaResourceManager.init();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ApiManager.stop();
        });

        // Initialize commands, events and API
        SInfoCommandManager.init();
        SInfoEventListener.init();        
        ApiManager.start();
    }
}

