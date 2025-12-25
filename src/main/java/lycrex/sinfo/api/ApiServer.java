package lycrex.sinfo.api;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.utils.JsonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ApiServer {
    private HttpServer server;
    private final int port;
    private final List<Consumer<ApiServer>> routes = new ArrayList<>();

    public ApiServer(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                
                // Register routes
                for (Consumer<ApiServer> routeSetter : routes) {
                    routeSetter.accept(this);
                }

                server.setExecutor(null);
                server.start();
                SInfoMod.LOGGER.info("SINFO API Server started on port: {}", port);
            } catch (IOException e) {
                SInfoMod.LOGGER.error("Failed to start API server", e);
            }
        }, "SInfo-Thread").start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public void addRoute(String path, HttpHandler handler) {
        if (server != null) {
            server.createContext(path, handler);
        } else {
            routes.add(api -> api.addRoute(path, handler));
        }
    }

    public static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }
        }
        return result;
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (java.io.InputStream is = exchange.getRequestBody();
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        }
    }

    public static void sendSuccess(HttpExchange exchange, String dataJson) throws IOException {
        String wrappedResponse = JsonUtils.builder()
                .add("status", "success")
                .add("data", JsonParser.parseString(dataJson))
                .build();
        sendResponse(exchange, 200, wrappedResponse);
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String wrappedResponse = JsonUtils.builder()
                .add("status", "error")
                .add("message", message)
                .build();
        sendResponse(exchange, statusCode, wrappedResponse);
    }

    public static void sendFail(HttpExchange exchange, int statusCode, String message) throws IOException {
        String wrappedResponse = JsonUtils.builder()
                .add("status", "fail")
                .add("message", message)
                .build();
        sendResponse(exchange, statusCode, wrappedResponse);
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        sendBinaryResponse(exchange, statusCode, "application/json; charset=UTF-8", bytes);
    }

    public static void sendBinaryResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
        exchange.close();
    }
}
