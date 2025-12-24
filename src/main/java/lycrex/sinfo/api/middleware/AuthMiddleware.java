package lycrex.sinfo.api.middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lycrex.sinfo.api.ApiServer;
import lycrex.sinfo.config.ModConfig;

import java.io.IOException;

public class AuthMiddleware implements HttpHandler {
    private final HttpHandler next;

    public AuthMiddleware(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiServer.sendResponse(exchange, 204, "");
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String expectedHash = ModConfig.getInstance().getPasswordHash();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String providedHash = authHeader.substring(7);
            if (providedHash.equals(expectedHash)) {
                next.handle(exchange);
                return;
            }
        }

        ApiServer.sendError(exchange, 401, "Unauthorized: Invalid or missing token");
    }

    public static HttpHandler wrap(HttpHandler handler) {
        return new AuthMiddleware(handler);
    }
}

