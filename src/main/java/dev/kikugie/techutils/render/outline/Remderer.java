package dev.kikugie.techutils.render.outline;

import dev.kikugie.techutils.mixin.containerscan.WorldRendererAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Remderer {
    private static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0, Set.of(Direction.values()));
    private static final RenderLayer RENDER_TYPE = RenderLayer.getOutline(new Identifier("textures/misc/white.png"));
    private static final Set<BlockPos> BLOCK_ESPS = ConcurrentHashMap.newKeySet();
    public static boolean isRendering = false;

    public static void render(MatrixStack matrices, Vec3d pos, OutlineVertexConsumerProvider vertexConsumers) {
        matrices.push();
        matrices.translate(-pos.x, -pos.y, -pos.z);
        renderBlockOutlines(matrices, pos, vertexConsumers);
        matrices.pop();
    }

    public static void renderBlockOutlines(MatrixStack matrices, Vec3d playerPos, OutlineVertexConsumerProvider vertexConsumers) {
        for (BlockPos pos : BLOCK_ESPS) {
            int cx = (int) (playerPos.x * 10 % 255);
            int cy = (int) (playerPos.y * 10 % 255);
            int cz = (int) (playerPos.z * 10 % 255);
            vertexConsumers.setColor(cx, cy, cz, 255);
            double squareDist = playerPos.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
            if (squareDist > 8 * 8) continue;

            matrices.push();
            matrices.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            matrices.push();
            matrices.translate(-0.5, -0.5, -0.5);

            ClientWorld world = MinecraftClient.getInstance().world;
            BlockState state = world.getBlockState(pos);
            List<Box> boundingBoxes = new ArrayList<>(state.getOutlineShape(world, pos).getBoundingBoxes());
            List<ModelPart.Cuboid> cuboids = new ArrayList<>();
            for (Box box : boundingBoxes)
                cuboids.add(compile(box));
            cuboids.forEach(cuboid -> cuboid.renderCuboid(matrices.peek(), vertexConsumers.getBuffer(RENDER_TYPE), LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.field_32955, 1, 1, 1, 1));

            matrices.pop();
            matrices.pop();
        }
    }

    private static ModelPart.Cuboid compile(Box box) {
        float sizeX = (float) (box.maxX - box.minX);
        float sizeY = (float) (box.maxY - box.minY);
        float sizeZ = (float) (box.maxZ - box.minZ);
        return new ModelPart.Cuboid(0, 0, (float) box.minX * 16, (float) box.minY * 16, (float) box.minZ * 16, sizeX * 16, sizeY * 16, sizeZ * 16, 0, 0, 0, false, 0, 0, Set.of(Direction.values()));
    }

    public static void onRender(WorldRenderContext context) {
        var worldRenderer = (WorldRendererAccessor) context.worldRenderer();
        if (!isRendering) {

            if (!BLOCK_ESPS.isEmpty()) {
                worldRenderer.getEntityOutlinePostProcessor().render(context.tickDelta());
                MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
            }
        }
        render(context.matrixStack(), context.camera().getPos(), worldRenderer.getBufferBuilders().getOutlineVertexConsumers());
        isRendering = false;
    }

    /**
     * Makes block glow.
     *
     * @param blockPos block position to mark as glowing.
     */
    public static void markBlock(BlockPos blockPos) {
        BLOCK_ESPS.add(blockPos);
    }


    /**
     * Clears all block positions
     * that outline is being rendered for.
     */
    public static void reset() {
        synchronized (BLOCK_ESPS) {
            BLOCK_ESPS.clear();
        }
    }

    /**
     * Stops rendering outline for given block cs_position.
     *
     * @param pos block cs_position
     */
    public static void removeBlockPos(BlockPos pos) {
        BLOCK_ESPS.remove(pos);
    }
}
