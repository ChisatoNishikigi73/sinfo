package lycrex.sinfo.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class SInfoCommandManager {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SInfoCommand.register(dispatcher);
        });
    }
}

