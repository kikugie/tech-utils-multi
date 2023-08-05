package dev.kikugie.techutils.feature.worldedit;

import com.mojang.brigadier.CommandDispatcher;
import dev.kikugie.techutils.TechUtilsMod;
import dev.kikugie.techutils.config.WorldEditConfigs;
import dev.kikugie.techutils.util.ResponseMuffler;
import dev.kikugie.techutils.util.ValidBox;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WorldEditSync {
    @Nullable
    private static WorldEditSync instance = null;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final WorldEditNetworkHandler handler = WorldEditNetworkHandler.initHandler();
    private ValidBox lastBox;
    private int syncTick = 0;
    private boolean worldEditConnected = false;
    private boolean commandAvailable = false;
    private boolean initComplete = false;

    public static void init() {
        instance = new WorldEditSync();
    }

    public static Optional<WorldEditSync> getInstance() {
        return Optional.ofNullable(instance);
    }

    public void onTick() {
        boolean initCompleteCache = !initComplete && worldEditConnected && commandAvailable;
        initComplete = worldEditConnected && commandAvailable;
        if (initCompleteCache)
            disableNeighborUpdates();
        if (initComplete)
            syncSelection();
    }

    public void onWorldEditConnected() {
        worldEditConnected = true;
    }

    public void onCommandTreePacket(CommandDispatcher<CommandSource> dispatcher) {
        commandAvailable = dispatcher.findNode(List.of("/pos1")) != null;
    }

    private void disableNeighborUpdates() {
        if (!WorldEditConfigs.DISABLE_UPDATES.getBooleanValue())
            return;
        ResponseMuffler.scheduleMute("Side effect \"Neighbors\".+");
        Objects.requireNonNull(client.getNetworkHandler()).sendCommand("/perf neighbors off");
        TechUtilsMod.LOGGER.debug("Turning off WorldEdit neighbor updates");
    }

    private void syncSelection() {
        if (!WorldEditConfigs.WE_SYNC.getBooleanValue())
            return;

        if (syncTick > 0) syncTick--;
        if (!handler.storage.isCuboid())
            return;

        Optional<ValidBox> boxOptional = getActiveSelection();
        if (boxOptional.isEmpty())
            return;
        ValidBox box = boxOptional.get();

        if (!box.equals(lastBox)) {
            lastBox = box;
            syncTick = WorldEditConfigs.WE_SYNC_TICKS.getIntegerValue();
            return;
        }
        if (syncTick != 0)
            return;

        syncTick = -1;
        updateRegion(box);
        TechUtilsMod.LOGGER.debug("WorldEdit synced!");
        if (WorldEditConfigs.WE_SYNC_FEEDBACK.getBooleanValue())
            client.player.sendMessage(Text.translatable("techutils.feature.worldeditsync.success").formatted(Formatting.GREEN), true);
    }

    private void updateRegion(ValidBox box) {
        ResponseMuffler.scheduleMute("(\\w+ position set to \\(.+\\).)|(Position already set.)");
        client.getNetworkHandler().sendCommand(String.format("/pos1 %d,%d,%d",
                box.getPos1().getX(),
                box.getPos1().getY(),
                box.getPos1().getZ()));
        ResponseMuffler.scheduleMute("(\\w+ position set to \\(.+\\).)|(Position already set.)");
        client.getNetworkHandler().sendCommand(String.format("/pos2 %d,%d,%d",
                box.getPos2().getX(),
                box.getPos2().getY(),
                box.getPos2().getZ()));
    }

    private Optional<ValidBox> getActiveSelection() {
        var selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) {
            return Optional.empty();
        }
        var box = selection.getSelectedSubRegionBox();
        if (box == null || box.getPos1() == null || box.getPos2() == null) {
            return Optional.empty();
        }
        return Optional.of(new ValidBox(box.getPos1(), box.getPos2(), "region"));
    }
}
