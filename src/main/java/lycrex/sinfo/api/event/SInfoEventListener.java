package lycrex.sinfo.api.event;

import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.utils.JsonUtils;
import lycrex.sinfo.utils.Platform;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public class SInfoEventListener {
    private static int tickCounter = 0;

    public static void init() {
        // Player Join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            int onlineCount = server.getCurrentPlayerCount();
            if (!server.getPlayerManager().getPlayerList().contains(handler.getPlayer())) {
                onlineCount += 1;
            }

            String data = JsonUtils.builder()
                    .add("event", "player_join")
                    .add("name", handler.getPlayer().getName().getString())
                    .add("uuid", handler.getPlayer().getUuidAsString())
                    .add("online", onlineCount)
                    .build();
            EventManager.broadcast("player_join", data);
        });

        // Player Leave
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String data = JsonUtils.builder()
                    .add("event", "player_leave")
                    .add("name", handler.getPlayer().getName().getString())
                    .add("uuid", handler.getPlayer().getUuidAsString())
                    .add("online", server.getCurrentPlayerCount() - 1)
                    .build();
            EventManager.broadcast("player_leave", data);
        });

        // Chat Message
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String data = JsonUtils.builder()
                    .add("event", "player_chat")
                    .add("sender", sender.getName().getString())
                    .add("text", message.getContent().getString())
                    .build();
            EventManager.broadcast("player_chat", data);
        });

        // Performance Heartbeat
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= 10 * 60 * 20) { // 10 minutes
                tickCounter = 0;
                sendPerformanceHeartbeat(server);
            }
        });
    }

    private static void sendPerformanceHeartbeat(MinecraftServer server) {
        double avgTickTime = Platform.getMspt(server);
        double tps = Math.min(20.0, 1000.0 / Math.max(1.0, avgTickTime));

        long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;

        String data = JsonUtils.builder()
                .add("event", "server_status")
                .add("tps", tps)
                .add("mspt", avgTickTime)
                .add("usedMemory", usedMemory)
                .add("totalMemory", totalMemory)
                .add("players", server.getCurrentPlayerCount())
                .build();
        EventManager.broadcast("server_status", data);
    }
}
