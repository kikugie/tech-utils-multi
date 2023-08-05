package dev.kikugie.techutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fi.dy.masa.litematica.data.DataManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class IsorenderSelectionCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess ignoredAccess) {
        if (FabricLoader.getInstance().isModLoaded("isometric-renders")) {
            dispatcher.register(literal("isorender").then(literal("selection").executes(IsorenderSelectionCommand::renderLitematicaSelection)));
        }
    }

    private static int renderLitematicaSelection(CommandContext<FabricClientCommandSource> context) {
        var selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) {
            context.getSource().sendError(Text.of("No Litematica selection!"));
            return 0;
        }
        var box = selection.getSelectedSubRegionBox();
        if (box == null) {
            context.getSource().sendError(Text.of("Invalid Litematica selection!"));
            return 0;
        }

        ClientCommandInternals.executeCommand(String.format("isorender area %d %d %d %d %d %d",
                box.getPos1().getX(),
                box.getPos1().getY(),
                box.getPos1().getZ(),
                box.getPos2().getX(),
                box.getPos2().getY(),
                box.getPos2().getZ()));

        return 0;
    }
}
