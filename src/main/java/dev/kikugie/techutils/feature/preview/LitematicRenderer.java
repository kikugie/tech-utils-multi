package dev.kikugie.techutils.feature.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;

//#if MC > 12000
import com.mojang.blaze3d.systems.VertexSorter;
//#endif

public class LitematicRenderer {
    private static final double MAX_BLOCK_WIDTH = Math.cos(Math.PI / 6) * 2;
    private final MinecraftClient client;
    private final LitematicMesh mesh;
    private final int slant;
    private final double horizontalSize;
    private final double verticalSize;
    public int angle = 135;

    public LitematicRenderer(WidgetFileBrowserBase.DirectoryEntry entry, GuiSchematicBrowserBase gui, int slant) {
        this.client = MinecraftClient.getInstance();
        this.slant = slant;
        LitematicaSchematic schematic = null;
        FileType fileType = FileType.fromFile(entry.getFullPath());
        switch (fileType) {
            case LITEMATICA_SCHEMATIC ->
                    schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName());
            case SCHEMATICA_SCHEMATIC ->
                    schematic = WorldUtils.convertSchematicaSchematicToLitematicaSchematic(entry.getDirectory(), entry.getName(), false, gui);
            case SPONGE_SCHEMATIC ->
                    schematic = WorldUtils.convertSpongeSchematicToLitematicaSchematic(entry.getDirectory(), entry.getName());
            case VANILLA_STRUCTURE ->
                    schematic = WorldUtils.convertStructureToLitematicaSchematic(entry.getDirectory(), entry.getName());
        }

        this.mesh = new LitematicMesh(Objects.requireNonNull(schematic));
        var shortestSide = Math.min(mesh.getSize().getX(), mesh.getSize().getZ());
        var longestSide = Math.max(mesh.getSize().getX(), mesh.getSize().getZ());
        this.horizontalSize = shortestSide * MAX_BLOCK_WIDTH + longestSide - shortestSide;
        this.verticalSize = longestSide * Math.tan(Math.toRadians(slant)) + mesh.getSize().getY();
    }

    public void render(MatrixStack matrices, int x, int y, int size) {
        angle++;
        if (mesh == null) return;
        RenderUtils.drawOutlinedBox(x, y, size, size, -1610612736, -6710887);
        if (mesh.isComplete()) {
            renderMesh(matrices, x, y, size);
        }
    }

    private void renderMesh(MatrixStack matrices, int x, int y, int size) {
        final Window window = MinecraftClient.getInstance().getWindow();
        final float aspectRatio = window.getFramebufferWidth() / (float) window.getFramebufferHeight();

        RenderSystem.backupProjectionMatrix();
        Matrix4f projectionMatrix = new Matrix4f().setOrtho(-aspectRatio, aspectRatio, -1, 1, -1000, 3000);

        //#if MC > 12000
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.BY_Z);
        //#else
        //$$ RenderSystem.setProjectionMatrix(projectionMatrix);
        //#endif
        matrices.push();
        matrices.loadIdentity();

        translateToCoords(matrices, x + size / 2, y + size / 2);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(slant));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        float scaleFactor = getScaleFactor(size);
        matrices.scale(scaleFactor, scaleFactor, scaleFactor);
        RenderSystem.applyModelViewMatrix();
        emitVertices();
        drawModel(matrices.peek().getPositionMatrix());
        matrices.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private void drawModel(Matrix4f viewMatrix) {
        drawLight(viewMatrix);
        MatrixStack meshStack = new MatrixStack();
        meshStack.multiplyPositionMatrix(viewMatrix);
        mesh.render(meshStack);
    }

    private void drawLight(Matrix4f viewMatrix) {
        final var lightTransform = new Matrix4f(viewMatrix);
        Vector4f lightDirection = new Vector4f(1F, 0.35F, 0, 0.0F);
        lightTransform.invert();
        lightDirection.mul(lightTransform);

        final var transformedLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);
        RenderSystem.setShaderLights(transformedLightDirection, transformedLightDirection);

        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
    }

    private void emitVertices() {
        var matrices = new MatrixStack();
        var vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        matrices.loadIdentity();

        final var blockEntities = mesh.getBlockEntities();
        blockEntities.forEach((blockPos, entity) -> {
            matrices.push();
            matrices.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            client.getBlockEntityRenderDispatcher().render(entity, 0, matrices, vertexConsumers);
            matrices.pop();
        });

        final var entities = mesh.getEntities();
        entities.forEach(entry -> {
            matrices.push();
            Vec3d pos = entry.entity().getPos();

            matrices.translate(pos.x, pos.y, pos.z);
            float tickDelta = client.getTickDelta();
            client.getEntityRenderDispatcher().render(entry.entity(), pos.x, pos.y, pos.z, entry.entity().getYaw(tickDelta), tickDelta, matrices, vertexConsumers, entry.light());
            matrices.pop();
        });

        drawLight(RenderSystem.getModelViewMatrix());
    }

    private void translateToCoords(MatrixStack matrixStack, int x, int y) {
        final Screen screen = MinecraftClient.getInstance().currentScreen;
        assert screen != null;
        final int w = screen.width;
        final int h = screen.height;
        matrixStack.translate((2f * x - w) / h, -(2f * y - h) / h, 0);
    }

    private float getScaleFactor(int size) {
        assert MinecraftClient.getInstance().currentScreen != null;
        return (float) ((size * 2) / (Math.max(horizontalSize, verticalSize) * MinecraftClient.getInstance().currentScreen.height));
    }
}
