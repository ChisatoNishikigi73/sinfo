package lycrex.sinfo.api.handler.player;

import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lycrex.sinfo.SInfoMod;
import lycrex.sinfo.api.ApiServer;
import lycrex.sinfo.utils.JsonUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerInfoHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        MinecraftServer server = SInfoMod.getServerInstance();
        if (server == null) {
            ApiServer.sendError(exchange, 500, "Server not ready");
            return;
        }

        Map<String, String> params = ApiServer.parseQuery(exchange.getRequestURI().getQuery());
        String nameParam = params.get("name");
        String uuidParam = params.get("uuid");

        UUID uuid = null;
        String name = null;

        // 1. 尝试解析 UUID 或从名字获取 UUID
        if (uuidParam != null) {
            try {
                uuid = UUID.fromString(uuidParam);
            } catch (IllegalArgumentException e) {
                ApiServer.sendFail(exchange, 400, "Invalid UUID format");
                return;
            }
        } else if (nameParam != null) {
            var profile = server.getUserCache().findByName(nameParam);
            if (profile.isPresent()) {
                uuid = profile.get().getId();
                name = profile.get().getName();
            }
        }

        if (uuid == null) {
            ApiServer.sendFail(exchange, 404, "Player not found");
            return;
        }

        // 2. 检查是否在线
        ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(uuid);
        if (onlinePlayer != null) {
            handleOnlinePlayer(exchange, onlinePlayer);
            return;
        }

        // 3. 处理离线玩家 (读取 NBT)
        handleOfflinePlayer(exchange, server, uuid, name);
    }

    private void handleOnlinePlayer(HttpExchange exchange, ServerPlayerEntity player) throws IOException {
        try {
            JsonUtils.Builder builder = JsonUtils.builder()
                    .add("name", player.getName().getString())
                    .add("uuid", player.getUuidAsString())
                    .add("isOnline", true)
                    .add("health", player.getHealth())
                    .add("maxHealth", player.getMaxHealth())
                    .add("foodLevel", player.getHungerManager().getFoodLevel())
                    .add("level", player.experienceLevel)
                    .add("xp", player.experienceProgress)
                    .add("gamemode", player.interactionManager.getGameMode().getName())
                    .add("dimension", player.getWorld().getRegistryKey().getValue().toString())
                    .add("pos", JsonUtils.builder()
                            .add("x", player.getX())
                            .add("y", player.getY())
                            .add("z", player.getZ())
                            .buildElement());

            // Inventory
            JsonArray inventoryArray = new JsonArray();
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    inventoryArray.add(serializeItemStack(stack, i));
                }
            }
            builder.add("inventory", inventoryArray);
            builder.add("mainHand", serializeItemStack(player.getMainHandStack(), -1));
            builder.add("offHand", serializeItemStack(player.getOffHandStack(), -1));

            ApiServer.sendSuccess(exchange, builder.build());
        } catch (Exception e) {
            SInfoMod.LOGGER.error("Failed to get online player details", e);
            ApiServer.sendError(exchange, 500, "Internal Server Error");
        }
    }

    private void handleOfflinePlayer(HttpExchange exchange, MinecraftServer server, UUID uuid, String name) throws IOException {
        File userDataFile = new File(server.getSavePath(WorldSavePath.PLAYERDATA).toFile(), uuid.toString() + ".dat");
        if (!userDataFile.exists()) {
            ApiServer.sendFail(exchange, 404, "Player data file not found");
            return;
        }

        if (name == null) {
            var profile = server.getUserCache().getByUuid(uuid);
            name = profile.isPresent() ? profile.get().getName() : "Unknown";
        }

        try {
            NbtCompound nbt = NbtIo.readCompressed(userDataFile);
            
            // 解析坐标
            NbtList posList = nbt.getList("Pos", NbtElement.DOUBLE_TYPE);
            double x = posList.getDouble(0);
            double y = posList.getDouble(1);
            double z = posList.getDouble(2);

            JsonUtils.Builder builder = JsonUtils.builder()
                    .add("name", name)
                    .add("uuid", uuid.toString())
                    .add("isOnline", false)
                    .add("health", nbt.getFloat("Health"))
                    .add("foodLevel", nbt.getInt("foodLevel"))
                    .add("level", nbt.getInt("XpLevel"))
                    .add("xp", nbt.getFloat("XpP"))
                    .add("dimension", nbt.getString("Dimension"))
                    .add("pos", JsonUtils.builder()
                            .add("x", x)
                            .add("y", y)
                            .add("z", z)
                            .buildElement());

            // 离线背包解析稍微复杂一点，暂不实现详细物品名，只返回 ID 和数量
            JsonArray inventoryArray = new JsonArray();
            NbtList invList = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < invList.size(); i++) {
                NbtCompound itemNbt = invList.getCompound(i);
                inventoryArray.add(JsonUtils.builder()
                        .add("id", itemNbt.getString("id"))
                        .add("count", itemNbt.getByte("Count"))
                        .add("slot", itemNbt.getByte("Slot"))
                        .buildElement());
            }
            builder.add("inventory", inventoryArray);

            ApiServer.sendSuccess(exchange, builder.build());
        } catch (Exception e) {
            SInfoMod.LOGGER.error("Failed to read offline player NBT", e);
            ApiServer.sendError(exchange, 500, "Internal Server Error");
        }
    }

    private com.google.gson.JsonElement serializeItemStack(ItemStack stack, int slot) {
        JsonUtils.Builder builder = JsonUtils.builder()
                .add("id", net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString())
                .add("count", stack.getCount())
                .add("name", stack.getName().getString());
        if (slot != -1) {
            builder.add("slot", slot);
        }
        return builder.buildElement();
    }
}

