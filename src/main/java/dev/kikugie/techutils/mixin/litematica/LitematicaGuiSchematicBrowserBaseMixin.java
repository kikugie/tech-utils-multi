package dev.kikugie.techutils.mixin.litematica;

import dev.kikugie.techutils.feature.preview.LitematicRenderManager;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiSchematicBrowserBase.class)
public abstract class LitematicaGuiSchematicBrowserBaseMixin extends GuiListBase {
    protected LitematicaGuiSchematicBrowserBaseMixin(int listX, int listY) {
        super(listX, listY);
    }

    @Override
    protected void closeGui(boolean showParent) {
        LitematicRenderManager.reset();
        super.closeGui(showParent);
    }

    @Override
    public void initGui() {
        LitematicRenderManager.init((GuiSchematicBrowserBase) (Object) this);
        super.initGui();
    }
}
