package dev.kikugie.techutils.mixin.litematica;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.feature.preview.LitematicRenderManager;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WidgetSchematicBrowser.class)
class WidgetSchematicBrowserMixin {
    @ModifyExpressionValue(method = "drawSelectedSchematicInfo", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object drawPreview(
            Object original,
            @Local(argsOnly = true) @Nullable WidgetFileBrowserBase.DirectoryEntry entry,
            @Local(ordinal = 0) int x,
            @Local(ordinal = 1) int y,
            @Local(ordinal = 2) int height
    ) {
        // TODO: If disabled in config return original
        LitematicRenderManager.getInstance().setCurrentRenderer(entry);
        LitematicRenderManager.getInstance().renderCurrent(x + 4, y + 14, height - y - 2);
        return null;
    }
}
