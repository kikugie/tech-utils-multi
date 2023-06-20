package dev.kikugie.techutils.mixin.litematica;

import dev.kikugie.techutils.feature.preview.LitematicRenderManager;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.Map;

@Mixin(WidgetSchematicBrowser.class)
class LitematicaWidgetSchematicBrowserMixin {
    @Final
    @Shadow(remap = false)
    protected Map<File, Pair<Identifier, NativeImageBackedTexture>> cachedPreviewImages;

    @Inject(method = "drawSelectedSchematicInfo", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
//    private void drawPreview(WidgetFileBrowserBase.DirectoryEntry entry, MatrixStack matrixStack, CallbackInfo ci, int x, int y, int height) {
    private void drawPreview(WidgetFileBrowserBase.DirectoryEntry entry, DrawContext drawContext, CallbackInfo ci, int x, int y, int height, SchematicMetadata meta, int textColor, int valueColor, String str, String strDate, Vec3i areaSize, String tmp) {
        LitematicRenderManager.getInstance().setCurrentRenderer(entry);
        LitematicRenderManager.getInstance().renderCurrent(x + 4, y + 14, height - y - 2);
        ci.cancel();
    }
}
