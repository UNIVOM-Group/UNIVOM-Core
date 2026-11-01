package com.mrlacar.univom.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public final class SmoothShapes {

    public static final float TWO_PI = (float) (Math.PI * 2.0);

    private static final byte RECT = 0;
    private static final byte STROKE = 1;
    private static final byte SECTOR = 2;
    private static final byte RAMP = 3;
    private static final byte ELLIPSE = 4;

    private static final float TOLERANCE = 0.1F;
    private static final int MIN_SEGMENTS = 4;
    private static final int MAX_SEGMENTS = 256;
    private static final float MIN_FEATHER = 0.05F;
    private static final float MAX_FEATHER = 1.0F;
    private static final float EPSILON = 1.0E-5F;

    public static final SmoothShapes NONE = new SmoothShapes(new byte[0], new int[0], new float[0]);

    private final byte[] kinds;
    private final int[] palette;
    private final float[] data;

    private SmoothShapes(byte[] kinds, int[] palette, float[] data) {
        this.kinds = kinds;
        this.palette = palette;
        this.data = data;
    }

    public void render(int originX, int originY, GuiGraphics graphics, int[] colors) {
        if (kinds.length == 0) {
            return;
        }
        graphics.flush();
        emit(originX, originY, graphics, colors);
        graphics.flush();
    }

    private void emit(int originX, int originY, GuiGraphics graphics, int[] colors) {
        Matrix4f matrix = graphics.pose().last().pose();
        float feather = Mth.clamp(1.0F / density(matrix), MIN_FEATHER, MAX_FEATHER);
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        int cursor = 0;
        for (int i = 0; i < kinds.length; i++) {
            int color = colors[palette[i]];
            float red = FastColor.ARGB32.red(color) / 255.0F;
            float green = FastColor.ARGB32.green(color) / 255.0F;
            float blue = FastColor.ARGB32.blue(color) / 255.0F;
            float alpha = FastColor.ARGB32.alpha(color) / 255.0F;
            switch (kinds[i]) {
                case RECT -> {
                    rect(consumer, matrix, originX + data[cursor], originY + data[cursor + 1],
                            originX + data[cursor + 2], originY + data[cursor + 3], red, green, blue, alpha);
                    cursor += 4;
                }
                case STROKE -> {
                    stroke(consumer, matrix, originX + data[cursor], originY + data[cursor + 1],
                            originX + data[cursor + 2], originY + data[cursor + 3], data[cursor + 4],
                            data[cursor + 5], feather, red, green, blue, alpha);
                    cursor += 6;
                }
                case SECTOR -> {
                    sector(consumer, matrix, originX + data[cursor], originY + data[cursor + 1], data[cursor + 2],
                            data[cursor + 3], data[cursor + 4], feather, red, green, blue, alpha);
                    cursor += 5;
                }
                case RAMP -> {
                    ramp(consumer, matrix, originX + data[cursor], originY + data[cursor + 1], data[cursor + 2],
                            data[cursor + 3], data[cursor + 4], feather, red, green, blue, alpha);
                    cursor += 5;
                }
                case ELLIPSE -> {
                    ellipse(consumer, matrix, originX + data[cursor], originY + data[cursor + 1], data[cursor + 2],
                            data[cursor + 3], data[cursor + 4], data[cursor + 5], data[cursor + 6], feather,
                            red, green, blue, alpha);
                    cursor += 7;
                }
                default -> {
                    return;
                }
            }
        }
    }

    private static float density(Matrix4f matrix) {
        double gui = Minecraft.getInstance().getWindow().getGuiScale();
        float determinant = Math.abs(matrix.m00() * matrix.m11() - matrix.m01() * matrix.m10());
        return determinant < EPSILON ? (float) gui : (float) (gui * Math.sqrt(determinant));
    }

    private static int segments(float arc, float radius, float feather) {
        return Mth.clamp(Mth.ceil(arc / Mth.sqrt(8.0F * radius * TOLERANCE * feather)), MIN_SEGMENTS, MAX_SEGMENTS);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float a0, float x1,
                             float y1, float a1, float x2, float y2, float a2, float x3, float y3, float a3,
                             float red, float green, float blue) {
        vertex(consumer, matrix, x0, y0, a0, red, green, blue);
        vertex(consumer, matrix, x1, y1, a1, red, green, blue);
        vertex(consumer, matrix, x2, y2, a2, red, green, blue);
        vertex(consumer, matrix, x3, y3, a3, red, green, blue);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float alpha, float red,
                               float green, float blue) {
        consumer.vertex(matrix.m00() * x + matrix.m10() * y + matrix.m30(),
                        matrix.m01() * x + matrix.m11() * y + matrix.m31(),
                        matrix.m02() * x + matrix.m12() * y + matrix.m32())
                .color(red, green, blue, alpha).endVertex();
    }

    private static void rect(VertexConsumer consumer, Matrix4f matrix, float left, float top, float right,
                             float bottom, float red, float green, float blue, float alpha) {
        quad(consumer, matrix, left, top, alpha, left, bottom, alpha, right, bottom, alpha, right, top, alpha,
                red, green, blue);
    }

    private static void stroke(VertexConsumer consumer, Matrix4f matrix, float ax, float ay, float bx, float by,
                               float halfA, float halfB, float feather, float red, float green, float blue,
                               float alpha) {
        float dx = bx - ax;
        float dy = by - ay;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < EPSILON) {
            return;
        }
        float normalX = -dy / length;
        float normalY = dx / length;
        float edge = feather * 0.5F;
        float thinnest = Math.min(halfA, halfB) * 2.0F;
        if (thinnest < feather) {
            alpha *= Math.max(thinnest, EPSILON) / feather;
            halfA = Math.max(halfA, edge);
            halfB = Math.max(halfB, edge);
        }
        float coreA = Math.max(0.0F, halfA - edge);
        float coreB = Math.max(0.0F, halfB - edge);
        float outerA = halfA + edge;
        float outerB = halfB + edge;

        if (coreA > EPSILON || coreB > EPSILON) {
            quad(consumer, matrix,
                    ax - normalX * coreA, ay - normalY * coreA, alpha,
                    ax + normalX * coreA, ay + normalY * coreA, alpha,
                    bx + normalX * coreB, by + normalY * coreB, alpha,
                    bx - normalX * coreB, by - normalY * coreB, alpha, red, green, blue);
        }
        quad(consumer, matrix,
                ax + normalX * coreA, ay + normalY * coreA, alpha,
                ax + normalX * outerA, ay + normalY * outerA, 0.0F,
                bx + normalX * outerB, by + normalY * outerB, 0.0F,
                bx + normalX * coreB, by + normalY * coreB, alpha, red, green, blue);
        quad(consumer, matrix,
                ax - normalX * outerA, ay - normalY * outerA, 0.0F,
                ax - normalX * coreA, ay - normalY * coreA, alpha,
                bx - normalX * coreB, by - normalY * coreB, alpha,
                bx - normalX * outerB, by - normalY * outerB, 0.0F, red, green, blue);
    }

    private static void sector(VertexConsumer consumer, Matrix4f matrix, float centerX, float centerY, float radius,
                               float from, float to, float feather, float red, float green, float blue,
                               float alpha) {
        float span = to - from;
        if (span <= EPSILON || radius <= EPSILON) {
            return;
        }
        float edge = feather * 0.5F;
        float core = Math.max(0.0F, radius - edge);
        float outer = radius + edge;
        int count = segments(span * radius, radius, feather);
        if ((count & 1) == 1) {
            count++;
        }
        float step = span / count;
        float sin = Mth.sin(from);
        float cos = Mth.cos(from);
        float previousX = centerX + sin * core;
        float previousY = centerY - cos * core;
        float previousOuterX = centerX + sin * outer;
        float previousOuterY = centerY - cos * outer;
        float pairX = previousX;
        float pairY = previousY;

        for (int i = 1; i <= count; i++) {
            float angle = i == count ? to : from + step * i;
            sin = Mth.sin(angle);
            cos = Mth.cos(angle);
            float coreX = centerX + sin * core;
            float coreY = centerY - cos * core;
            float outerX = centerX + sin * outer;
            float outerY = centerY - cos * outer;
            quad(consumer, matrix, previousX, previousY, alpha, coreX, coreY, alpha, outerX, outerY, 0.0F,
                    previousOuterX, previousOuterY, 0.0F, red, green, blue);
            if ((i & 1) == 0) {
                quad(consumer, matrix, centerX, centerY, alpha, coreX, coreY, alpha, previousX, previousY, alpha,
                        pairX, pairY, alpha, red, green, blue);
                pairX = coreX;
                pairY = coreY;
            }
            previousX = coreX;
            previousY = coreY;
            previousOuterX = outerX;
            previousOuterY = outerY;
        }
    }

    private static void ramp(VertexConsumer consumer, Matrix4f matrix, float centerX, float centerY, float radius,
                             float angle, float side, float feather, float red, float green, float blue,
                             float alpha) {
        float edge = feather * 0.5F;
        float length = radius - edge;
        if (length <= EPSILON) {
            return;
        }
        float axisX = Mth.sin(angle);
        float axisY = -Mth.cos(angle);
        float sideX = Mth.cos(angle);
        float sideY = Mth.sin(angle);
        float low = side < 0.0F ? -edge : 0.0F;
        float high = side < 0.0F ? 0.0F : edge;
        float lowAlpha = side < 0.0F ? 0.0F : alpha * 0.5F;
        float highAlpha = side < 0.0F ? alpha * 0.5F : 0.0F;
        float farX = centerX + axisX * length;
        float farY = centerY + axisY * length;
        quad(consumer, matrix,
                centerX + sideX * low, centerY + sideY * low, lowAlpha,
                centerX + sideX * high, centerY + sideY * high, highAlpha,
                farX + sideX * high, farY + sideY * high, highAlpha,
                farX + sideX * low, farY + sideY * low, lowAlpha, red, green, blue);
    }

    private static void ellipse(VertexConsumer consumer, Matrix4f matrix, float centerX, float centerY, float m00,
                                float m01, float m10, float m11, float halfWidth, float feather, float red,
                                float green, float blue, float alpha) {
        if (Math.abs(m00 * m11 - m01 * m10) < EPSILON) {
            return;
        }
        float edge = feather * 0.5F;
        float core = Math.max(0.0F, halfWidth - edge);
        float outer = halfWidth + edge;
        float mean = Mth.sqrt((m00 * m00 + m01 * m01 + m10 * m10 + m11 * m11) * 0.5F);
        int count = segments(TWO_PI * mean, mean, feather);
        float step = TWO_PI / count;

        float previousX = centerX + m00;
        float previousY = centerY + m01;
        float previousNormalX = 0.0F;
        float previousNormalY = 0.0F;
        boolean ready = false;

        for (int i = 0; i <= count; i++) {
            float angle = i == count ? 0.0F : step * i;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            float x = centerX + m00 * cos + m10 * sin;
            float y = centerY + m01 * cos + m11 * sin;
            float tangentX = m10 * cos - m00 * sin;
            float tangentY = m11 * cos - m01 * sin;
            float length = Mth.sqrt(tangentX * tangentX + tangentY * tangentY);
            if (length < EPSILON) {
                continue;
            }
            float normalX = tangentY / length;
            float normalY = -tangentX / length;
            if (ready) {
                quad(consumer, matrix,
                        previousX - previousNormalX * outer, previousY - previousNormalY * outer, 0.0F,
                        x - normalX * outer, y - normalY * outer, 0.0F,
                        x - normalX * core, y - normalY * core, alpha,
                        previousX - previousNormalX * core, previousY - previousNormalY * core, alpha,
                        red, green, blue);
                if (core > EPSILON) {
                    quad(consumer, matrix,
                            previousX - previousNormalX * core, previousY - previousNormalY * core, alpha,
                            x - normalX * core, y - normalY * core, alpha,
                            x + normalX * core, y + normalY * core, alpha,
                            previousX + previousNormalX * core, previousY + previousNormalY * core, alpha,
                            red, green, blue);
                }
                quad(consumer, matrix,
                        previousX + previousNormalX * core, previousY + previousNormalY * core, alpha,
                        x + normalX * core, y + normalY * core, alpha,
                        x + normalX * outer, y + normalY * outer, 0.0F,
                        previousX + previousNormalX * outer, previousY + previousNormalY * outer, 0.0F,
                        red, green, blue);
            }
            previousX = x;
            previousY = y;
            previousNormalX = normalX;
            previousNormalY = normalY;
            ready = true;
        }
    }

    public static final class Collector {

        private byte[] kinds = new byte[32];
        private int[] palette = new int[32];
        private float[] data = new float[192];
        private int count;
        private int size;

        public void rect(float left, float top, float right, float bottom, int paletteId) {
            push(RECT, paletteId, 4);
            data[size++] = left;
            data[size++] = top;
            data[size++] = right;
            data[size++] = bottom;
        }

        public void stroke(float ax, float ay, float bx, float by, float halfA, float halfB, int paletteId) {
            push(STROKE, paletteId, 6);
            data[size++] = ax;
            data[size++] = ay;
            data[size++] = bx;
            data[size++] = by;
            data[size++] = halfA;
            data[size++] = halfB;
        }

        public void sector(float centerX, float centerY, float radius, float from, float to, int paletteId) {
            push(SECTOR, paletteId, 5);
            data[size++] = centerX;
            data[size++] = centerY;
            data[size++] = radius;
            data[size++] = from;
            data[size++] = to;
        }

        public void ramp(float centerX, float centerY, float radius, float angle, float side, int paletteId) {
            push(RAMP, paletteId, 5);
            data[size++] = centerX;
            data[size++] = centerY;
            data[size++] = radius;
            data[size++] = angle;
            data[size++] = side;
        }

        public void ellipse(float centerX, float centerY, float m00, float m01, float m10, float m11,
                            float halfWidth, int paletteId) {
            push(ELLIPSE, paletteId, 7);
            data[size++] = centerX;
            data[size++] = centerY;
            data[size++] = m00;
            data[size++] = m01;
            data[size++] = m10;
            data[size++] = m11;
            data[size++] = halfWidth;
        }

        public SmoothShapes build() {
            if (count == 0) {
                return NONE;
            }
            return new SmoothShapes(Arrays.copyOf(kinds, count), Arrays.copyOf(palette, count),
                    Arrays.copyOf(data, size));
        }

        private void push(byte kind, int paletteId, int stride) {
            if (count == kinds.length) {
                kinds = Arrays.copyOf(kinds, count * 2);
                palette = Arrays.copyOf(palette, count * 2);
            }
            if (size + stride > data.length) {
                data = Arrays.copyOf(data, Math.max(size + stride, data.length * 2));
            }
            kinds[count] = kind;
            palette[count] = paletteId;
            count++;
        }
    }
}
