package lycrex.sinfo.api.event;

import lycrex.sinfo.utils.JsonUtils;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Minecraft Version: 1.19.3
 */
public class AdvancementHandler {

    /**
     * 处理玩家成就达成事件并广播
     * Supported: 1.16 - 1.20.1
     */
    public static void handleAdvancement(ServerPlayerEntity player, Object advancementObj) {
        if (advancementObj instanceof Advancement advancement) {
            if (advancement.getDisplay() != null) {
                String title = advancement.getDisplay().getTitle().getString();
                String description = advancement.getDisplay().getDescription().getString();
                String type = advancement.getDisplay().getFrame().name().toLowerCase();

                String data = JsonUtils.builder()
                        .add("event", "player_advancement")
                        .add("name", player.getName().getString())
                        .add("uuid", player.getUuidAsString())
                        .add("title", title)
                        .add("description", description)
                        .add("type", type)
                        .build();
                
                EventManager.broadcast("player_advancement", data);
            }
        }
    }
}
