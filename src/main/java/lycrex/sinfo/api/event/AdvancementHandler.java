package lycrex.sinfo.api.event;

import lycrex.sinfo.utils.JsonUtils;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Minecraft Version: 1.20.2
 */
public class AdvancementHandler {

    /**
     * 处理玩家成就达成事件并广播
     * Supported: 1.20.2+
     */
    public static void handleAdvancement(ServerPlayerEntity player, Object advancementObj) {
        if (advancementObj instanceof AdvancementEntry advancementEntry) {
            Optional<AdvancementDisplay> displayOptional = advancementEntry.value().display();
            if (displayOptional.isPresent()) {
                AdvancementDisplay display = displayOptional.get();
                String title = display.getTitle().getString();
                String description = display.getDescription().getString();
                String type = display.getFrame().name().toLowerCase();

                String data = JsonUtils.builder()
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
