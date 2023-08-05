package dev.kikugie.techutils.render.outline;

import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class Line {
    public static final float WIDTH = 2.0f;
    public final Vec3d start;
    public final Vec3d end;
    public final Color4f color;

    public Line(Vec3d start, Vec3d end, Color4f color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    public void render(MatrixStack matrixStack, VertexConsumer vertexConsumer) {
        Vec3d normal = end.subtract(start).normalize();
        vertex(matrixStack, vertexConsumer, start, normal);
        vertex(matrixStack, vertexConsumer, end, normal);
    }

    private void vertex(MatrixStack matrices, VertexConsumer consumer, Vec3d pos, Vec3d normal) {
        consumer.vertex(matrices.peek().getPositionMatrix(),
                        (float) pos.getX(),
                        (float) pos.getY(),
                        (float) pos.getZ())
                .color(color.r, color.g, color.b, color.a)
                .normal(matrices.peek().getNormalMatrix(),
                        (float) normal.getX(),
                        (float) normal.getY(),
                        (float) normal.getZ())
                .next();
    }
}
