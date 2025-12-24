package lycrex.sinfo.api.handler.player;

import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.api.ApiServer;
import lycrex.sinfo.utils.JsonUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerListHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        MinecraftServer server = SInfoMod.getServerInstance();
        if (server == null) {
            ApiServer.sendError(exchange, 500, "Server not ready");
            return;
        }

        Map<String, String> params = ApiServer.parseQuery(exchange.getRequestURI().getQuery());
        String type = params.getOrDefault("type", "online");

        try {
            JsonArray playersArray = new JsonArray();
            Set<UUID> addedUuids = new HashSet<>();

            // 1. 处理在线玩家
            if ("online".equals(type) || "all".equals(type)) {
                List<ServerPlayerEntity> onlinePlayers = server.getPlayerManager().getPlayerList();
                for (ServerPlayerEntity player : onlinePlayers) {
                    playersArray.add(JsonUtils.builder()
                            .add("name", player.getName().getString())
                            .add("uuid", player.getUuidAsString())
                            .add("isOnline", true)
                            .add("health", player.getHealth())
                            .add("maxHealth", player.getMaxHealth())
                            .add("level", player.experienceLevel)
                            .buildElement());
                    addedUuids.add(player.getUuid());
                }
            }

            // 2. 处理离线玩家
            if ("offline".equals(type) || "all".equals(type)) {
                // 获取 playerdata 目录
                File userDataDir = server.getSavePath(net.minecraft.util.WorldSavePath.PLAYERDATA).toFile();
                if (userDataDir.exists() && userDataDir.isDirectory()) {
                    File[] files = userDataDir.listFiles((dir, name) -> name.endsWith(".dat"));
                    if (files != null) {
                        for (File file : files) {
                            String fileName = file.getName();
                            String uuidStr = fileName.substring(0, fileName.length() - 4); // 移除 .dat
                            try {
                                UUID uuid = UUID.fromString(uuidStr);
                                if (addedUuids.contains(uuid)) continue;

                                var profile = server.getUserCache().getByUuid(uuid);
                                String name = profile.isPresent() ? profile.get().getName() : "Unknown";

                                playersArray.add(JsonUtils.builder()
                                        .add("name", name)
                                        .add("uuid", uuidStr)
                                        .add("isOnline", false)
                                        .buildElement());
                                addedUuids.add(uuid);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
            }

            String dataJson = JsonUtils.builder()
                    .add("count", playersArray.size())
                    .add("players", playersArray)
                    .build();

            ApiServer.sendSuccess(exchange, dataJson);
        } catch (Exception e) {
            SInfoMod.LOGGER.error("Failed to get player list", e);
            ApiServer.sendError(exchange, 500, "Internal Server Error");
        }
    }
}
