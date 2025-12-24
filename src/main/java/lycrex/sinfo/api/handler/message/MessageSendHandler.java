package lycrex.sinfo.api.handler.message;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.api.ApiServer;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageSendHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiServer.sendFail(exchange, 405, "Method not allowed, use POST");
            return;
        }

        MinecraftServer server = SInfoMod.getServerInstance();
        if (server == null) {
            ApiServer.sendError(exchange, 500, "Server not ready");
            return;
        }

        try {
            String body = ApiServer.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String target = json.has("target") ? json.get("target").getAsString() : "all";
            String type = json.has("type") ? json.get("type").getAsString() : "chat";
            String content = json.has("message") ? json.get("message").getAsString() : "";

            List<ServerPlayerEntity> targets = new ArrayList<>();
            if ("all".equalsIgnoreCase(target)) {
                targets.addAll(server.getPlayerManager().getPlayerList());
            } else {
                ServerPlayerEntity player = null;
                try {
                    player = server.getPlayerManager().getPlayer(UUID.fromString(target));
                } catch (IllegalArgumentException e) {
                    player = server.getPlayerManager().getPlayer(target);
                }
                if (player != null) {
                    targets.add(player);
                }
            }

            if (targets.isEmpty()) {
                ApiServer.sendFail(exchange, 404, "Target player(s) not found");
                return;
            }

            for (ServerPlayerEntity player : targets) {
                Text messageText = parseText(content);
                switch (type.toLowerCase()) {
                    case "chat":
                        player.sendMessage(messageText, false);
                        break;
                    case "actionbar":
                        player.sendMessage(messageText, true);
                        break;
                    case "title":
                        String subContent = json.has("subtitle") ? json.get("subtitle").getAsString() : "";
                        int fadeIn = json.has("fadeIn") ? json.get("fadeIn").getAsInt() : 10;
                        int stay = json.has("stay") ? json.get("stay").getAsInt() : 70;
                        int fadeOut = json.has("fadeOut") ? json.get("fadeOut").getAsInt() : 20;

                        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
                        player.networkHandler.sendPacket(new TitleS2CPacket(messageText));
                        if (!subContent.isEmpty()) {
                            player.networkHandler.sendPacket(new SubtitleS2CPacket(parseText(subContent)));
                        }
                        break;
                    default:
                        ApiServer.sendFail(exchange, 400, "Unknown message type: " + type);
                        return;
                }
            }

            ApiServer.sendSuccess(exchange, "{\"sent\": " + targets.size() + "}");
        } catch (Exception e) {
            SInfoMod.LOGGER.error("Error sending message", e);
            ApiServer.sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private Text parseText(String content) {
        if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
            try {
                return Text.Serializer.fromJson(content);
            } catch (Exception e) {
                return Text.literal(content);
            }
        }
        return Text.literal(content);
    }
}

