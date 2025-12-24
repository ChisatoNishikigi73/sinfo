package lycrex.sinfo.api.handler.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.api.ApiServer;
import lycrex.sinfo.utils.JsonUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandExecuteHandler implements HttpHandler {
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

            if (!json.has("command")) {
                ApiServer.sendFail(exchange, 400, "Missing 'command' field");
                return;
            }

            String command = json.get("command").getAsString();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            final List<String> feedback = new ArrayList<>();
            
            // Create a custom command source to capture feedback
            ServerCommandSource source = server.getCommandSource()
                .withReturnValueConsumer((value) -> {})
                .withOutput(new net.minecraft.server.command.CommandOutput() {
                    @Override
                    public void sendMessage(Text message) {
                        feedback.add(message.getString());
                    }

                    @Override
                    public boolean shouldReceiveFeedback() {
                        return true;
                    }

                    @Override
                    public boolean shouldTrackOutput() {
                        return true;
                    }

                    @Override
                    public boolean shouldBroadcastConsoleToOps() {
                        return false;
                    }
                });

            server.getCommandManager().executeWithPrefix(source, command);

            String response = JsonUtils.builder()
                    .add("command", command)
                    .add("feedback", String.join("\n", feedback))
                    .build();

            ApiServer.sendSuccess(exchange, response);
        } catch (Exception e) {
            SInfoMod.LOGGER.error("Error executing command via API", e);
            ApiServer.sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}

