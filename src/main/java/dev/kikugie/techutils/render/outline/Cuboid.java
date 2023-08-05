package dev.kikugie.techutils.render.outline;

import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Cuboid {
    private final Line[] lines = new Line[12];
    private final Vec3d pos;

    public Cuboid(Box box, Color4f color) {
        this(new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.maxX, box.maxY, box.maxZ), color);
    }

    public Cuboid(Vec3d start, Vec3d end, Color4f color) {
        this.pos = start;
        Vec3d size = end.subtract(start);
        this.lines[0] = new Line(start, start.add(size.x, 0, 0), color);
        this.lines[1] = new Line(start, start.add(0, size.y, 0), color);
        this.lines[2] = new Line(start, start.add(0, 0, size.z), color);

        this.lines[3] = new Line(start.add(size.x, 0, size.z), start.add(size.x, 0, 0), color);
        this.lines[4] = new Line(start.add(size.x, 0, size.z), start.add(size.x, size.y, size.z), color);
        this.lines[5] = new Line(start.add(size.x, 0, size.z), start.add(0, 0, size.z), color);

        this.lines[6] = new Line(start.add(size.x, size.y, 0), start.add(size.x, 0, 0), color);
        this.lines[7] = new Line(start.add(size.x, size.y, 0), start.add(0, size.y, 0), color);
        this.lines[8] = new Line(start.add(size.x, size.y, 0), start.add(size.x, size.y, size.z), color);

        this.lines[9] = new Line(start.add(0, size.y, size.z), start.add(0, size.y, 0), color);
        this.lines[10] = new Line(start.add(0, size.y, size.z), start.add(0, 0, size.z), color);
        this.lines[11] = new Line(start.add(0, size.y, size.z), start.add(size.x, size.y, size.z), color);
    }


    public void render(MatrixStack matrixStack, VertexConsumer vertexConsumer) {
        for (Line line : lines) {
            line.render(matrixStack, vertexConsumer);
        }
    }
}
