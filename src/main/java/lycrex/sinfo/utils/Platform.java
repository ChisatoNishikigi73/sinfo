package lycrex.sinfo.utils;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Platform Support for Minecraft 1.19.3
 */
public class Platform {

    /**
     * 发送命令反馈消息
     * Supported: 1.16 - 1.19.4
     */
    public static void sendFeedback(ServerCommandSource source, Text text, boolean broadcastToOps) {
        source.sendFeedback(text, broadcastToOps);
    }

    /**
     * 获取带有自定义输出的命令源
     * Supported: 1.16+
     */
    public static ServerCommandSource getSourceWithOutput(ServerCommandSource source, CommandOutput output) {
        return source.withOutput(output);
    }

    /**
     * 获取服务器平均每刻耗时 (MSPT)
     * Supported: 1.16 - 1.20.3
     */
    public static double getMspt(MinecraftServer server) {
        return server.getTickTime();
    }

    /**
     * 获取物品注册表
     * Supported: 1.19.3+
     */
    public static Registry<Item> getItemRegistry() {
        return Registries.ITEM;
    }
}
