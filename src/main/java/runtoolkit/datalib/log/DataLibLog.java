package runtoolkit.datalib.log;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataLibLog implements ModInitializer {

    public static final String MOD_ID = "datalib-log";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Poll interval: every 20 ticks (1 second).
    // The datapack log ring buffer holds 30 entries; polling at 20t prevents overflow.
    private static final int POLL_INTERVAL_TICKS = 20;
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("[DataLib] Log bridge initialised.");

        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    private void onTick(MinecraftServer server) {
        if (++tickCounter < POLL_INTERVAL_TICKS) return;
        tickCounter = 0;

        var commandStorage = server.getCommandStorage();

        // datalib:engine storage — key: log_display (NbtList of {text, color} compounds)
        NbtCompound engineStorage = commandStorage.get("datalib", "engine");
        if (engineStorage == null || !engineStorage.contains("log_display", NbtElement.LIST_TYPE)) return;

        NbtList logDisplay = engineStorage.getList("log_display", NbtElement.COMPOUND_TYPE);
        if (logDisplay.isEmpty()) return;

        for (NbtElement entry : logDisplay) {
            if (!(entry instanceof NbtCompound compound)) continue;
            String text  = compound.getString("text");
            String color = compound.getString("color");
            emitToServerLog(text, color);
        }

        // Clear the ring buffer after reading so we don't re-emit
        engineStorage.remove("log_display");
        commandStorage.set("datalib", "engine", engineStorage);
    }

    private void emitToServerLog(String text, String color) {
        // Route by prefix — level is embedded in text as "[LEVEL] message"
        if (text.startsWith("[ERROR]")) {
            LOGGER.error("[DataLib] {}", text);
        } else if (text.startsWith("[WARN]")) {
            LOGGER.warn("[DataLib] {}", text);
        } else if (text.startsWith("[DEBUG]")) {
            LOGGER.debug("[DataLib] {}", text);
        } else {
            LOGGER.info("[DataLib] {}", text);
        }
    }
}
