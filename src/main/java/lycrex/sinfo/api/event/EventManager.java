package lycrex.sinfo.api.event;

import com.sun.net.httpserver.HttpExchange;
import lycrex.sinfo.SInfoMod;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventManager {
    private static final List<HttpExchange> clients = Collections.synchronizedList(new ArrayList<>());

    public static void addClient(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        try {
            exchange.sendResponseHeaders(200, 0);
            clients.add(exchange);
            SInfoMod.LOGGER.info("New SSE client connected. Total: {}", clients.size());
        } catch (IOException e) {
            SInfoMod.LOGGER.error("Failed to add SSE client", e);
        }
    }

    public static void broadcast(String eventType, String data) {
        String message = String.format("event: %s\ndata: %s\n\n", eventType, data);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        synchronized (clients) {
            clients.removeIf(exchange -> {
                try {
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.flush();
                    return false;
                } catch (IOException e) {
                    SInfoMod.LOGGER.info("Client disconnected from SSE stream");
                    return true;
                }
            });
        }
    }

    public static void shutdown() {
        synchronized (clients) {
            for (HttpExchange exchange : clients) {
                exchange.close();
            }
            clients.clear();
        }
    }
}

