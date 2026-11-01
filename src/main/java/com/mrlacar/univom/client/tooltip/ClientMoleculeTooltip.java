package com.mrlacar.univom.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class ClientMoleculeTooltip implements ClientTooltipComponent {

    private final MoleculeLayout layout;

    public ClientMoleculeTooltip(MoleculeTooltip component) {
        this.layout = MoleculeLayout.of(component.molecule());
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return layout.width();
    }

    @Override
    public int getHeight() {
        return layout.height();
    }

    @Override
    public void renderText(@NotNull Font font, int x, int y, @NotNull Matrix4f matrix,
                           MultiBufferSource.@NotNull BufferSource buffer) {
        layout.renderText(font, x, y, matrix, buffer);
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics graphics) {
        layout.renderImage(x, y, graphics);
    }
}
