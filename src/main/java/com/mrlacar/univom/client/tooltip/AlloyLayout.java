package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class AlloyLayout {

    private static final double TWO_PI = Math.PI * 2.0;
    private static final float MIN_SLICE = 1.0E-4F;
    private static final int BASE_WIDTH = 200;
    private static final int WHITE_SLOT = 0;
    private static final Reference2ObjectMap<AlloySlices, AlloyLayout> CACHE = new Reference2ObjectOpenHashMap<>(32);

    private static int cachedRadius = -1;

    private final int width;
    private final int height;
    private final SmoothShapes shapes;
    private final FormattedCharSequence[] labels;
    private final int[] labelX;
    private final int[] labelY;
    private final Material[] palette;
    private final int[] paletteColors;

    private AlloyLayout(int width, int height, SmoothShapes shapes, FormattedCharSequence[] labels, int[] labelX,
                        int[] labelY, Material[] palette) {
        this.width = width;
        this.height = height;
        this.shapes = shapes;
        this.labels = labels;
        this.labelX = labelX;
        this.labelY = labelY;
        this.palette = palette;
        this.paletteColors = new int[palette.length];
    }

    public static void invalidate() {
        CACHE.clear();
        cachedRadius = -1;
    }

    public static AlloyLayout of(AlloySlices slices) {
        int radius = ConfigHolder.INSTANCE.moleculeTooltipSetting.alloyPieRadius;
        if (cachedRadius != radius) {
            CACHE.clear();
            cachedRadius = radius;
        }
        AlloyLayout layout = CACHE.get(slices);
        if (layout == null) {
            layout = bake(slices, radius);
            CACHE.put(slices, layout);
        }
        return layout;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffer) {
        for (int i = 0; i < labels.length; i++) {
            font.drawInBatch(labels[i], x + labelX[i], y + labelY[i], 0xFFFFFFFF, false, matrix, buffer,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
    }

    public void renderImage(int x, int y, GuiGraphics graphics) {
        paletteColors[WHITE_SLOT] = 0xFFFFFFFF;
        for (int i = 1; i < palette.length; i++) {
            paletteColors[i] = ChemColor.forMaterial(palette[i]);
        }
        shapes.render(x, y, graphics, paletteColors);
    }

    private static AlloyLayout bake(AlloySlices slices, int radius) {
        Font font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight;
        Material[] materials = slices.materials();
        double[] fractions = slices.fractions();
        int count = materials.length;

        double[] stops = new double[count];
        double[] centers = new double[count];
        double cumulative = 0.0;
        for (int i = 0; i < count; i++) {
            stops[i] = cumulative * TWO_PI;
            centers[i] = (cumulative + fractions[i] * 0.5) * TWO_PI;
            cumulative += fractions[i];
        }

        FormattedCharSequence[] labels = new FormattedCharSequence[count];
        int[] labelX = new int[count];
        int[] labelY = new int[count];
        boolean[] onLeft = new boolean[count];
        int[] anchorY = new int[count];

        int atMostY = Integer.MAX_VALUE;
        int atLeastY = Integer.MIN_VALUE;
        int widestLeft = 0;
        int widestRight = 0;

        for (int i = 0; i < count; i++) {
            MutableComponent text = Component.literal(percentage(fractions[i]) + " ")
                    .append(ChemColor.colorizeFormula(materials[i].getChemicalFormula()));
            labels[i] = text.getVisualOrderText();
            int textWidth = font.width(text);
            int sliceY = (int) (-Math.cos(centers[i]) * 0.9 * radius);
            boolean left = centers[i] > Math.PI;
            onLeft[i] = left;
            int edgeX = (left ? -1 : 1) * (radius + 10);
            if (left) {
                int topY = Math.min(sliceY - lineHeight / 2, atMostY);
                labelX[i] = edgeX - 5 - textWidth;
                labelY[i] = topY;
                atMostY = topY - lineHeight - 1;
                widestLeft = Math.max(widestLeft, textWidth);
            } else {
                int topY = Math.max(sliceY - lineHeight / 2, atLeastY);
                labelX[i] = edgeX + 5;
                labelY[i] = topY;
                atLeastY = topY + lineHeight + 1;
                widestRight = Math.max(widestRight, textWidth);
            }
            anchorY[i] = labelY[i] + lineHeight / 2;
        }

        int baseHeight = radius * 5 / 2;
        int addTop = Math.max(0, -atMostY - baseHeight / 2);
        int addBottom = Math.max(0, atLeastY - baseHeight / 2);
        int addLeft = Math.max(0, widestLeft + radius + 20 - BASE_WIDTH / 2);
        int addRight = Math.max(0, widestRight + radius + 20 - BASE_WIDTH / 2);

        int centerX = BASE_WIDTH / 2 + addLeft;
        int centerY = baseHeight / 2 + addTop;

        SmoothShapes.Collector shapes = new SmoothShapes.Collector();
        paintPie(shapes, stops, centerX + 0.5F, centerY + 0.5F, radius + 0.5F);

        for (int i = 0; i < count; i++) {
            int sliceX = centerX + (int) (Math.sin(centers[i]) * 0.9 * radius);
            int sliceY = centerY - (int) (Math.cos(centers[i]) * 0.9 * radius);
            int lineY = centerY + anchorY[i];
            int edgeX = centerX + (onLeft[i] ? -1 : 1) * (radius + 10);
            shapes.rect(sliceX, Math.min(sliceY, lineY), sliceX + 1, Math.max(sliceY, lineY) + 1, WHITE_SLOT);
            shapes.rect(Math.min(sliceX, edgeX), lineY, Math.max(sliceX, edgeX) + 1, lineY + 1, WHITE_SLOT);
            labelX[i] += centerX;
            labelY[i] += centerY;
        }

        Material[] palette = new Material[count + 1];
        System.arraycopy(materials, 0, palette, 1, count);

        return new AlloyLayout(BASE_WIDTH + addLeft + addRight, baseHeight + addTop + addBottom,
                shapes.build(), labels, labelX, labelY, palette);
    }

    private static void paintPie(SmoothShapes.Collector shapes, double[] stops, float centerX, float centerY,
                                 float radius) {
        int count = stops.length;
        for (int i = 0; i < count; i++) {
            if (span(stops, i) < MIN_SLICE) {
                continue;
            }
            shapes.sector(centerX, centerY, radius, (float) stops[i], end(stops, i), i + 1);
        }
        if (count < 2) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int before = i == 0 ? count - 1 : i - 1;
            if (span(stops, i) < MIN_SLICE || span(stops, before) < MIN_SLICE) {
                continue;
            }
            float angle = (float) stops[i];
            shapes.ramp(centerX, centerY, radius, angle, -1.0F, i + 1);
            shapes.ramp(centerX, centerY, radius, angle, 1.0F, before + 1);
        }
    }

    private static float end(double[] stops, int index) {
        return index + 1 < stops.length ? (float) stops[index + 1] : SmoothShapes.TWO_PI;
    }

    private static float span(double[] stops, int index) {
        return end(stops, index) - (float) stops[index];
    }

    private static String percentage(double fraction) {
        double percent = fraction * 100.0;
        return percent < 0.1 ? "<0.1%" : String.format("%.1f%%", percent);
    }
}
