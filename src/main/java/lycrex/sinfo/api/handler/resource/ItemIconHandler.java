package lycrex.sinfo.api.handler.resource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lycrex.sinfo.SInfoConstants;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.api.ApiServer;
import lycrex.sinfo.api.resource.VanillaResourceManager;
import lycrex.sinfo.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class ItemIconHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params = ApiServer.parseQuery(exchange.getRequestURI().getQuery());
        String idStr = params.get("id");

        if (idStr == null || idStr.isEmpty()) {
            ApiServer.sendFail(exchange, 400, "Missing item id");
            return;
        }

        try {
            Identifier id = new Identifier(idStr);
            
            // 1. 尝试从所有已加载的 JAR 中提取
            byte[] iconBytes = tryGetFromJars(id);
            if (iconBytes != null) {
                ApiServer.sendBinaryResponse(exchange, 200, "image/png", iconBytes);
                return;
            }

            // 2. 如果是原版物品
            if ("minecraft".equals(id.getNamespace())) {
                File cacheFile = VanillaResourceManager.getVanillaCache(id.getPath());
                if (cacheFile.exists()) {
                    ApiServer.sendBinaryResponse(exchange, 200, "image/png", Files.readAllBytes(cacheFile.toPath()));
                    return;
                }
                
                String fallbackUrl = String.format(SInfoConstants.VANILLA_ASSETS_URL_TEMPLATE, 
                        VanillaResourceManager.getMcVersion(), "item", id.getPath());
                exchange.getResponseHeaders().set("Location", fallbackUrl);
                ApiServer.sendResponse(exchange, 302, "");
                return;
            }

            ApiServer.sendFail(exchange, 404, "Texture not found for: " + idStr);
        } catch (Exception e) {
            SInfoMod.LOGGER.error("Failed to retrieve icon for " + idStr, e);
            ApiServer.sendError(exchange, 500, "Internal server error retrieving texture");
        }
    }

    private byte[] tryGetFromJars(Identifier id) {
        String itemPath = String.format("assets/%s/textures/item/%s.png", id.getNamespace(), id.getPath());
        byte[] bytes = getResourceBytes(id.getNamespace(), itemPath);
        
        if (bytes == null) {
            String blockPath = String.format("assets/%s/textures/block/%s.png", id.getNamespace(), id.getPath());
            bytes = getResourceBytes(id.getNamespace(), blockPath);
        }
        return bytes;
    }

    private byte[] getResourceBytes(String namespace, String path) {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(namespace);
        if (container.isPresent()) {
            Optional<Path> resource = container.get().findPath(path);
            if (resource.isPresent()) {
                try {
                    return Files.readAllBytes(resource.get());
                } catch (IOException ignored) {}
            }
        }
        return null;
    }
}
