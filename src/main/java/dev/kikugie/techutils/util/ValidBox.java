package dev.kikugie.techutils.util;

import fi.dy.masa.litematica.selection.Box;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An extension of {@link Box} that ensures corners are not null (to shut up the IDE). As well as provides a few utility methods I need.
 */
@SuppressWarnings("NullableProblems")
public class ValidBox extends Box {
    private BlockPos minPos;
    private BlockPos maxPos;

    public ValidBox(BlockPos pos1, BlockPos pos2, String name) {
        super(pos1, pos2, name);
        Validate.notNull(pos1);
        Validate.notNull(pos2);
    }

    public ValidBox(int[] corners) {
        super(new BlockPos(corners[0], corners[1], corners[2]), new BlockPos(corners[3], corners[4], corners[5]), "");
    }

    public static ValidBox of(Box box) {
        return new ValidBox(box.getPos1(), box.getPos2(), box.getName());
    }
    @NotNull
    @Override
    public BlockPos getPos1() {
        return Objects.requireNonNull(super.getPos1());
    }

    @NotNull
    @Override
    public BlockPos getPos2() {
        return Objects.requireNonNull(super.getPos2());
    }

    public BlockPos getPos1(BlockPos fallback) {
        BlockPos pos = super.getPos1();
        return pos != null ? pos : fallback;
    }

    public BlockPos getPos2(BlockPos fallback) {
        BlockPos pos = super.getPos2();
        return pos != null ? pos : fallback;
    }

    @Override
    public void setPos1(@NotNull BlockPos pos) {
        super.setPos1(pos);
        updateCorners();
    }

    @Override
    public void setPos2(@NotNull BlockPos pos) {
        super.setPos2(pos);
        updateCorners();
    }

    public BlockPos getMin() {
        if (minPos == null)
            updateCorners();
        return minPos;
    }

    public BlockPos getMax() {
        if (maxPos == null)
            updateCorners();
        return maxPos;
    }

    private void updateCorners() {
        BlockPos pos1 = getPos1(BlockPos.ORIGIN);
        BlockPos pos2 = getPos2(BlockPos.ORIGIN);

        int x1 = pos1.getX();
        int y1 = pos1.getY();
        int z1 = pos1.getZ();
        int x2 = pos2.getX();
        int y2 = pos2.getY();
        int z2 = pos2.getZ();

        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        minPos = new BlockPos(minX, minY, minZ);
        maxPos = new BlockPos(maxX, maxY, maxZ);
    }
}
