package lycrex.sinfo.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import lycrex.sinfo.config.ModConfig;
import lycrex.sinfo.utils.Platform;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SInfoCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sinfo")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(SInfoCommand::run));
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        var server = context.getSource().getServer();
        String mcVersion = net.minecraft.SharedConstants.getGameVersion().getName();
        ModConfig config = ModConfig.getInstance();
        
        String hostIp = "localhost";
        try {
            hostIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {}

        // TPS and Performance
        double avgTickTime = Platform.getMspt(server);
        double tps = Math.min(20.0, 1000.0 / Math.max(1.0, avgTickTime));
        
        // Player info
        int onlinePlayers = server.getCurrentPlayerCount();
        int maxPlayers = server.getMaxPlayerCount();

        String info = String.format(
            "§6[SInfo]§r\n" +
            "§7Version: §a%s§r\n" +
            "§7Performance: §a%.1f TPS §7(§a%.2f ms§7)\n" +
            "§7API Address: §bhttp://%s:%d§r\n" + 
            "§7Players: §a%d/%d§r",
            mcVersion, tps, avgTickTime, hostIp, config.apiPort, onlinePlayers, maxPlayers
        );
        Platform.sendFeedback(context.getSource(), Text.literal(info), false);
        return 1;
    }
}

