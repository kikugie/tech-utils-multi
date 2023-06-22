package dev.kikugie.techutils.mixin.litematica;

import dev.kikugie.techutils.feature.preview.LitematicRenderManager;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = GuiSchematicBrowserBase.class)
public abstract class GuiSchematicBrowserBaseMixin extends GuiListBase<WidgetFileBrowserBase.DirectoryEntry, WidgetDirectoryEntry, WidgetSchematicBrowser> {
    protected GuiSchematicBrowserBaseMixin(int listX, int listY) {
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
