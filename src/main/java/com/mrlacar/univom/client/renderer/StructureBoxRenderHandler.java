package com.mrlacar.univom.client.renderer;

import com.mrlacar.univom.common.item.StructureWandBehaviour;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class StructureBoxRenderHandler {
    public static void renderStructureSelect(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ItemStack held = mc.player.getMainHandItem();
        if (!StructureWandBehaviour.isItemStructureWriter(held)) return;

        BlockPos[] positions = StructureWandBehaviour.getPos(held);
        if (positions == null) return;

        BlockPos p1 = positions[0];
        BlockPos p2 = positions[1];
        Vec3 cameraPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        renderFilledCube(poseStack, p1, p2);

        renderOutlinedCube(poseStack, p1, p2);

        renderAxisMarker(poseStack, p1);

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();

        poseStack.popPose();
    }

    private static void renderFilledCube(PoseStack poseStack, BlockPos p1, BlockPos p2) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buffer = tess.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderBufferUtils.renderCubeFace(
                poseStack, buffer,
                p1.getX(), p1.getY(), p1.getZ(),
                p2.getX() + 1, p2.getY() + 1, p2.getZ() + 1,
                0.85f, 0.85f, 1f, 0.25f, true);

        tess.end();
    }

    private static void renderOutlinedCube(PoseStack poseStack, BlockPos p1, BlockPos p2) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buffer = tess.getBuilder();

        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(3);

        RenderBufferUtils.drawCubeFrame(
                poseStack, buffer,
                p1.getX(), p1.getY(), p1.getZ(),
                p2.getX() + 1, p2.getY() + 1, p2.getZ() + 1,
                1f, 1f, 1f, 0.5f);

        tess.end();
    }

    private static void renderAxisMarker(PoseStack poseStack, BlockPos origin) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buffer = tess.getBuilder();

        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(12);

        Matrix4f mat4 = poseStack.last().pose();
        Matrix3f mat3 = new Matrix3f(mat4);

        drawColoredPoint(buffer, mat4, mat3, origin, 1f, 0f, 0f);
        drawColoredPoint(buffer, mat4, mat3, origin, 0f, 1f, 0f);
        drawColoredPoint(buffer, mat4, mat3, origin, 0f, 0f, 1f);

        tess.end();
    }

    private static void drawColoredPoint(BufferBuilder buffer, Matrix4f mat4, Matrix3f mat3, BlockPos pos, float r,
                                         float g, float b) {
        buffer.vertex(mat4, pos.getX(), pos.getY(), pos.getZ())
                .color(r, g, b, 0.75f)
                .normal(mat3, 0, 0, 0)
                .endVertex();
        buffer.vertex(mat4, pos.getX(), pos.getY(), pos.getZ())
                .color(r, g, b, 0.75f)
                .normal(mat3, 0, 0, 0)
                .endVertex();
    }
}
