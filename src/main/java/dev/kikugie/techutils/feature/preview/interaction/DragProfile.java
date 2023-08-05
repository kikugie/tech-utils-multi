package dev.kikugie.techutils.feature.preview.interaction;


import dev.kikugie.techutils.config.LitematicConfigs;
import dev.kikugie.techutils.feature.preview.gui.PreviewRenderManager;

/**
 * Interaction profile implementing following functionality:<br>
 * <pre>
 * - Dragging left mouse click: rotate the model;
 * - Dragging right mouse click: move the model;
 * - Scrolling mouse wheel: scale the model.
 * </pre>
 */
public class DragProfile implements InteractionProfile {
    private final PreviewRenderManager manager;
    private int x;
    private int y;
    private int viewport;

    private boolean validDrag = false;
    private float dx = 0;
    private float dy = 0;
    private float scaleMod = 0;
    private float scale = 1;
    private double angle = Math.PI / 4;

    DragProfile(PreviewRenderManager manager) {
        this.manager = manager;
    }

    @Override
    public void set(int x, int y, int viewportSize) {
        this.x = x;
        this.y = y;
        this.viewport = viewportSize;
    }

    @Override
    public void scrolled(double x, double y, double amount) {
        if (!inViewport(x, y))
            return;
        scaleMod += amount * 0.5;
        scale = (float) Math.exp(scaleMod);
    }

    @Override
    public void dragged(double x, double y, double dx, double dy, int button) {
        if (!validDrag)
            return;

        switch (button) {
            case 0 -> angle += dx * 0.1 * LitematicConfigs.ROTATION_FACTOR.getDoubleValue();
            case 1 -> {
                this.dx += dx;
                this.dy += dy;
            }
            default -> {
            }
        }
    }

    @Override
    public void released(double x, double y) {
        validDrag = false;
    }

    @Override
    public void clicked(double x, double y, int button) {
        if (inViewport(x, y))
            validDrag = true;
    }

    @Override
    public boolean inViewport(double x, double y) {
        return x > this.x && y > this.y && x < this.x + this.viewport && y < this.y + this.viewport;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public int viewport() {
        return viewport;
    }

    @Override
    public double angle() {
        return angle;
    }

    @Override
    public float dx() {
        return dx;
    }

    @Override
    public float dy() {
        return dy;
    }

    @Override
    public float scale() {
        return scale;
    }
}
