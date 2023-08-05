package dev.kikugie.techutils.render.outline;

import fi.dy.masa.malilib.util.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import java.util.*;

public class WireframeRenderer {
    private static final Map<BlockPos, Collection<Cuboid>> entries = new Hashtable<>();
    public static RenderLayer NO_DEPTH = RenderLayer.of("techutils_nodepth", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 256, true, true, RenderLayer.MultiPhaseParameters.builder()
            .program(RenderLayer.LINES_PROGRAM)
            .writeMaskState(RenderLayer.COLOR_MASK)
            .cull(RenderLayer.DISABLE_CULLING)
            .depthTest(RenderLayer.ALWAYS_DEPTH_TEST)
            .layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
            .lineWidth(new RenderLayer.LineWidth(OptionalDouble.of(Line.WIDTH)))
            .build(true));

    public static void render(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        for (Collection<Cuboid> cuboids : entries.values())
            for (Cuboid cuboid : cuboids)
                cuboid.render(context.matrixStack(), Objects.requireNonNull(context.consumers()).getBuffer(NO_DEPTH));
        matrices.pop();
    }

    public static void add(BlockView world, Color4f color, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        List<Box> boundingBoxes = new ArrayList<>(state.getOutlineShape(world, pos).getBoundingBoxes());
        if (boundingBoxes.isEmpty())
            boundingBoxes.add(new Box(pos));
        else
            boundingBoxes.replaceAll(box -> box.offset(pos));
        List<Cuboid> cuboids = new ArrayList<>();
        for (Box box : boundingBoxes)
            cuboids.add(new Cuboid(box, color));
        entries.put(pos, cuboids);
    }

    public static void add(BlockView world, Color4f color, BlockPos... pos) {
        for (BlockPos p : pos)
            add(world, color, p);
    }

    public static void remove(BlockPos... pos) {
        for (BlockPos p : pos)
            entries.remove(p);
    }

    public static void retain(BlockPos... pos) {
        if (pos.length > 0)
            entries.keySet().retainAll(Arrays.asList(pos));
    }
}
