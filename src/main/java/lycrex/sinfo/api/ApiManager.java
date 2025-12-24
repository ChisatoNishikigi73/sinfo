package lycrex.sinfo.api;

import lycrex.sinfo.SInfoConstants;
import lycrex.sinfo.api.handler.message.MessageSendHandler;
import lycrex.sinfo.api.handler.player.PlayerInfoHandler;
import lycrex.sinfo.api.handler.player.PlayerListHandler;
import lycrex.sinfo.api.handler.command.CommandExecuteHandler;
import lycrex.sinfo.api.handler.resource.ItemIconHandler;
import lycrex.sinfo.api.handler.server.ServerInfoHandler;
import lycrex.sinfo.api.event.EventStreamHandler;
import lycrex.sinfo.api.middleware.AuthMiddleware;
import lycrex.sinfo.config.ModConfig;

public class ApiManager {
    private static ApiServer apiServer;

    public static void start() {
        ModConfig config = ModConfig.getInstance();
        apiServer = new ApiServer(config.apiPort);
        
        // Register all routes here
        registerRoutes();
        
        apiServer.start();
    }

    private static void registerRoutes() {
        // Player routes
        apiServer.addRoute(SInfoConstants.API_PREFIX + "/player/list", AuthMiddleware.wrap(new PlayerListHandler()));
        apiServer.addRoute(SInfoConstants.API_PREFIX + "/player/info", AuthMiddleware.wrap(new PlayerInfoHandler()));
        
        // Message routes
        apiServer.addRoute(SInfoConstants.API_PREFIX + "/message/send", AuthMiddleware.wrap(new MessageSendHandler()));
        
        // Command routes
        apiServer.addRoute(SInfoConstants.API_PREFIX + "/command/execute", AuthMiddleware.wrap(new CommandExecuteHandler()));

        // Server routes
        apiServer.addRoute(SInfoConstants.API_PREFIX + "/server/info", AuthMiddleware.wrap(new ServerInfoHandler()));

        // Event Stream
        apiServer.addRoute(SInfoConstants.API_PREFIX + "/event/stream", AuthMiddleware.wrap(new EventStreamHandler()));

        // Resource routes
        apiServer.addRoute(SInfoConstants.API_PREFIX + "/resource/item/icon", new ItemIconHandler());
    }

    public static void stop() {
        if (apiServer != null) {
            apiServer.stop();
        }
    }
}

