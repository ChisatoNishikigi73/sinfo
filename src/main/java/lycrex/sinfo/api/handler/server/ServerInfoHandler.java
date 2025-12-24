package lycrex.sinfo.api.handler.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.api.ApiServer;
import lycrex.sinfo.utils.JsonUtils;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;

public class ServerInfoHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        MinecraftServer server = SInfoMod.getServerInstance();
        if (server == null) {
            ApiServer.sendError(exchange, 500, "Server not ready");
            return;
        }

        try {
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;

            // TPS calculation (1.20 specific)
            double tps = Math.min(20.0, 1000.0 / Math.max(1.0, server.getTickTime()));

            String dataJson = JsonUtils.builder()
                    .add("version", net.minecraft.SharedConstants.getGameVersion().getName())
                    .add("motd", server.getServerMotd())
                    .add("seed", server.getOverworld().getSeed())
                    .add("difficulty", server.getSaveProperties().getDifficulty().getName().toLowerCase())
                    .add("players", JsonUtils.builder()
                            .add("online", server.getCurrentPlayerCount())
                            .add("max", server.getMaxPlayerCount())
                            .buildElement())
                    .add("performance", JsonUtils.builder()
                            .add("tps", tps)
                            .add("avgTickTime", server.getTickTime())
                            .buildElement())
                    .add("memory", JsonUtils.builder()
                            .add("used", usedMemory / 1024 / 1024) // MB
                            .add("total", totalMemory / 1024 / 1024) // MB
                            .buildElement())
                    .build();

            ApiServer.sendSuccess(exchange, dataJson);
        } catch (Exception e) {
            SInfoMod.LOGGER.error("Error getting server info", e);
            ApiServer.sendError(exchange, 500, "Internal Server Error");
        }
    }
}

