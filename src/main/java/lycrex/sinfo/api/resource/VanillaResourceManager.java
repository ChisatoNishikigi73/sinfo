package lycrex.sinfo.api.resource;

import lycrex.sinfo.SInfoConstants;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.config.ModConfig;
import lycrex.sinfo.utils.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VanillaResourceManager {
    private static String mcVersion = net.minecraft.SharedConstants.getGameVersion().getName();
    private static final File CACHE_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "sinfo/icons/vanilla");
    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(4);
    
    private static final AtomicInteger totalToDownload = new AtomicInteger(0);
    private static final AtomicInteger currentDownloaded = new AtomicInteger(0);

    public static void init() {
        if (!CACHE_DIR.exists()) {
            CACHE_DIR.mkdirs();
        }

        if (ModConfig.getInstance().downloadVanillaTextures) {
            startPreDownload();
        }
    }

    private static void startPreDownload() {
        new Thread(() -> {
            SInfoMod.LOGGER.info("Starting pre-download of vanilla textures...");
            int totalVanillaItems = 0;
            int tasksSubmitted = 0;

            for (Identifier id : Platform.getItemRegistry().getIds()) {
                if ("minecraft".equals(id.getNamespace())) {
                    totalVanillaItems++;
                    String itemName = id.getPath();
                    File cacheFile = new File(CACHE_DIR, itemName + ".png");
                    
                    if (!cacheFile.exists()) {
                        tasksSubmitted++;
                        totalToDownload.incrementAndGet();
                        DOWNLOAD_EXECUTOR.submit(() -> downloadItem(itemName, cacheFile));
                    }
                }
            }

            if (tasksSubmitted > 0) {
                SInfoMod.LOGGER.info("Pre-downloading {} missing textures (total {} vanilla items)...", tasksSubmitted, totalVanillaItems);
            } else {
                SInfoMod.LOGGER.info("All {} vanilla textures are already in local cache.", totalVanillaItems);
            }
            
            try {
                DOWNLOAD_EXECUTOR.shutdown();
                if (DOWNLOAD_EXECUTOR.awaitTermination(30, TimeUnit.MINUTES)) {
                    SInfoMod.LOGGER.info("All pre-download tasks completed.");
                }
            } catch (InterruptedException e) {
                SInfoMod.LOGGER.error("Pre-download process interrupted", e);
            }
        }, "SInfo-TextureDownloader").start();
    }

    private static void downloadItem(String itemName, File cacheFile) {
        boolean success = false;
        String itemUrl = String.format(SInfoConstants.VANILLA_ASSETS_URL_TEMPLATE, mcVersion, "item", itemName);
        if (tryDownload(itemUrl, cacheFile)) {
            success = true;
        } else {
            String blockUrl = String.format(SInfoConstants.VANILLA_ASSETS_URL_TEMPLATE, mcVersion, "block", itemName);
            if (tryDownload(blockUrl, cacheFile)) {
                success = true;
            }
        }

        int current = currentDownloaded.incrementAndGet();
        int total = totalToDownload.get();
        
        if (success) {
            SInfoMod.LOGGER.debug("[{}/{}] Downloaded vanilla texture: {}", current, total, itemName);
        } else {
            SInfoMod.LOGGER.debug("[{}/{}] Skipped vanilla texture (no image resource): {}", current, total, itemName);
        }
    }

    private static boolean tryDownload(String urlStr, File cacheFile) {
        try {
            URL url = new URL(urlStr);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == 200) {
                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, cacheFile.toPath());
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static File getVanillaCache(String itemName) {
        return new File(CACHE_DIR, itemName + ".png");
    }

    public static String getMcVersion() {
        return mcVersion;
    }
}

