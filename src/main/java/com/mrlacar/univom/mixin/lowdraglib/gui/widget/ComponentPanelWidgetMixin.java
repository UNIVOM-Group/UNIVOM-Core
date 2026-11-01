package com.mrlacar.univom.mixin.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(value = ComponentPanelWidget.class, remap = false)
public abstract class ComponentPanelWidgetMixin {
    @Shadow
    protected abstract Style getStyleUnderMouse(double mouseX, double mouseY);

    @Inject(method = "drawInForeground", at = @At("HEAD"), cancellable = true)
    private void injectItemHoverEvent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Style style = this.getStyleUnderMouse(mouseX, mouseY);

        if (style != null && style.getHoverEvent() != null) {
            HoverEvent hoverEvent = style.getHoverEvent();

            if (hoverEvent.getAction() == HoverEvent.Action.SHOW_ITEM) {
                graphics.renderComponentHoverEffect(Minecraft.getInstance().font, style, mouseX, mouseY);
                ci.cancel();
            }
        }
    }
}
