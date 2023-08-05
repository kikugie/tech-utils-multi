package dev.kikugie.techutils.feature.preview.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import dev.kikugie.techutils.TechUtilsMod;
import dev.kikugie.techutils.util.ValidBox;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
//#endif

/*
//#if MC > 12000
import com.mojang.blaze3d.systems.VertexSorter;
//#endif
 */

public class LitematicMesh {
    private final LitematicaSchematic schematic;
    private final MinecraftClient client;

    private final Map<RenderLayer, VertexBuffer> bufferStorage;
    private final Map<RenderLayer, BufferBuilder> initializedLayers;

    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private final List<EntityEntry> entities = new ArrayList<>();
    private final ValidBox fullBox;
    private final int totalBlocks;
    private int processed = 0;
    private boolean done = false;

    public LitematicMesh(LitematicaSchematic schematic) {
        this.schematic = schematic;
        this.client = MinecraftClient.getInstance();
        this.bufferStorage = new HashMap<>();
        this.initializedLayers = new HashMap<>();
        this.totalBlocks = schematic.getMetadata().getTotalBlocks();
        this.fullBox = fullBox(schematic.getAreas().values());

        CompletableFuture.runAsync(this::build, Util.getMainWorkerExecutor()).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                throw new RuntimeException(throwable);
            }
        });
    }

    private void build() {
        MatrixStack matrices = new MatrixStack();
        Random random = Random.createLocal();


        Vec3i corner = fullBox.getMin();
        Vec3i size = schematic.getTotalSize();

        Vec3d offset = new Vec3d(
                -corner.getX() - (double) size.getX() / 2d,
                -corner.getY() - (double) size.getY() / 2d,
                -corner.getZ() - (double) size.getZ() / 2d);
        matrices.translate(offset.x, offset.y, offset.z);

        try {
            CompletableFuture<List<EntityEntry>> entitiesFuture = new CompletableFuture<>();
            client.execute(() -> entitiesFuture.complete(readEntities()));


            schematic.getAreas().keySet().forEach(region -> buildRegion(matrices, region, random));

            if (initializedLayers.containsKey(RenderLayer.getTranslucent())) {
                var translucentBuilder = initializedLayers.get(RenderLayer.getTranslucent());
                //#if MC > 12000
                translucentBuilder.setSorter(VertexSorter.byDistance(0, 0, 1000));
                //#else
                //$$ translucentBuilder.sortFrom(0, 0, 1000);
                //#endif
            }

            var future = new CompletableFuture<Void>();
            RenderSystem.recordRenderCall(() -> {
                initializedLayers.forEach((renderLayer, bufferBuilder) -> {
                    //#if MC > 12000
                    final var vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    //#else
                    //$$ final var vertexBuffer = new VertexBuffer();
                    //#endif

                    vertexBuffer.bind();
                    vertexBuffer.upload(bufferBuilder.end());

                    bufferStorage.put(renderLayer, vertexBuffer);
                });

                future.complete(null);
            });
            future.join();

            entitiesFuture.join().forEach(entry -> {
                Vec3d newPos = entry.entity.getPos().add(offset);
                entry.entity().updatePosition(newPos.x, newPos.y, newPos.z);
                entities.add(entry);
            });

            entities.addAll(entitiesFuture.join());
            done = true;
        } catch (Exception e) {
            TechUtilsMod.LOGGER.error("Rendering gone wrong:\n", e);
        }
    }

    private List<EntityEntry> readEntities() {
        ArrayList<EntityEntry> entities = new ArrayList<>();
        schematic.getAreas().keySet().forEach(region -> {
            List<LitematicaSchematic.EntityInfo> schematicEntities = schematic.getEntityListForRegion(region);
            assert schematicEntities != null;

            schematicEntities.forEach(entityInfo -> entities.add(new EntityEntry(
                    EntityUtils.createEntityAndPassengersFromNBT(entityInfo.nbt, client.world),
                    0xFF00FF)));
        });
        return entities;
    }

    private void buildRegion(MatrixStack matrices, String region, Random random) {
        Box box = schematic.getAreas().get(region);
        RegionBlockView view = new RegionBlockView(
                Objects.requireNonNull(schematic.getSubRegionContainer(region)),
                box);
        Map<BlockPos, NbtCompound> schematicBlockEntities = schematic.getBlockEntityMapForRegion(region);

        assert schematicBlockEntities != null;
        assert client.player != null;

        BlockRenderManager manager = client.getBlockRenderManager();
        PreviewFluidRenderer fluidRenderer = new PreviewFluidRenderer();

        for (BlockPos pos : BlockPos.iterate(view.box.getPos1(), view.box.getPos2())) {
            BlockState state = view.getBlockState(pos);
            if (state.isAir())
                continue;

            if (state.getBlock() instanceof BlockEntityProvider) {
                BlockEntity blockEntity = BlockEntity.createFromNbt(client.player.getBlockPos(), state, schematicBlockEntities.getOrDefault(pos, new NbtCompound()));
                if (blockEntity != null)
                    blockEntities.put(pos.toImmutable(), blockEntity);
            }

            if (!state.getFluidState().isEmpty()) {
                FluidState fluidState = state.getFluidState();

                RenderLayer fluidLayer = RenderLayers.getFluidLayer(fluidState);

                matrices.push();
                matrices.translate(-(pos.getX() & 15), -(pos.getY() & 15), -(pos.getZ() & 15));
                matrices.translate(pos.getX(), pos.getY(), pos.getZ());

                fluidRenderer.setMatrix(matrices.peek().getPositionMatrix());
                fluidRenderer.render(view, pos, getOrCreateBuffer(fluidLayer), state, fluidState);
                matrices.pop();
            }

            matrices.push();
            matrices.translate(pos.getX(), pos.getY(), pos.getZ());

            BakedModel model = manager.getModel(state);
            RenderLayer renderLayer = RenderLayers.getBlockLayer(state);

            if (state.getRenderType() == BlockRenderType.MODEL) {
                manager.getModelRenderer().render(view, model, state, pos, matrices, getOrCreateBuffer(renderLayer), true, random, state.getRenderingSeed(pos), OverlayTexture.DEFAULT_UV);
            }

            matrices.pop();
            processed++;
        }
    }

    private VertexConsumer getOrCreateBuffer(RenderLayer layer) {
        if (!initializedLayers.containsKey(layer)) {
            BufferBuilder builder = new BufferBuilder(layer.getExpectedBufferSize());
            initializedLayers.put(layer, builder);
            builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
        }
        return initializedLayers.get(layer);
    }

    public void render(MatrixStack matrices) {
        final var matrix = matrices.peek().getPositionMatrix();

        final RenderLayer translucent = RenderLayer.getTranslucent();
        bufferStorage.forEach((renderLayer, vertexBuffer) -> {
            if (renderLayer == translucent) return;
            draw(renderLayer, vertexBuffer, matrix);
        });

        if (bufferStorage.containsKey(translucent)) {
            draw(translucent, bufferStorage.get(translucent), matrix);
        }

        VertexBuffer.unbind();
    }

    private void draw(RenderLayer renderLayer, VertexBuffer vertexBuffer, Matrix4f matrix) {
        renderLayer.startDrawing();

        vertexBuffer.bind();
        vertexBuffer.draw(matrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

        renderLayer.endDrawing();
    }

    private ValidBox fullBox(Collection<Box> boxes) {
        int[] corners = {0, 0, 0, 0, 0, 0};
        for (Box box : boxes) {
            ValidBox validBox = ValidBox.of(box);
            BlockPos min = validBox.getMin();
            BlockPos max = validBox.getMax();

            corners[0] = Math.min(corners[0], min.getX());
            corners[1] = Math.min(corners[1], min.getY());
            corners[2] = Math.min(corners[2], min.getZ());
            corners[3] = Math.max(corners[3], max.getX());
            corners[4] = Math.max(corners[4], max.getY());
            corners[5] = Math.max(corners[5], max.getZ());
        }
        return new ValidBox(corners);
    }

    public Map<BlockPos, BlockEntity> blockEntities() {
        return blockEntities;
    }

    public List<EntityEntry> entities() {
        return entities;
    }

    public Vec3i size() {
        return schematic.getTotalSize();
    }

    public boolean complete() {
        return done;
    }

    public float progress() {
        return processed / (float) totalBlocks;
    }

    public record EntityEntry(Entity entity, int light) {
    }
}
