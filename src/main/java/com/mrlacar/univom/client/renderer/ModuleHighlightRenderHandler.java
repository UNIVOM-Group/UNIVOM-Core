package com.mrlacar.univom.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.world.client.ModuleHighlightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
@OnlyIn(Dist.CLIENT)
public class ModuleHighlightRenderHandler {

    private static final long CLEANUP_INTERVAL_MS = 1000L;
    private static long nextCleanupMs = 0L;

    public static class CustomRenderTypes extends RenderType {
        private CustomRenderTypes(String name, VertexFormat fmt, VertexFormat.Mode mode, int bufSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, fmt, mode, bufSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        public static final RenderType HIGHLIGHT_BOX = create(
                "univom_highlight_box",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Map<BlockPos, ModuleHighlightManager.Highlight> highlights = ModuleHighlightManager.HIGHLIGHT_POSITIONS;
        if (highlights.isEmpty()) return;

        long now = System.currentTimeMillis();

        if (now >= nextCleanupMs) {
            nextCleanupMs = now + CLEANUP_INTERVAL_MS;
            highlights.values().removeIf(highlight -> now > highlight.expiresAt());
            if (highlights.isEmpty()) return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer filledConsumer = bufferSource.getBuffer(CustomRenderTypes.HIGHLIGHT_BOX);
        for (ModuleHighlightManager.Highlight highlight : highlights.values()) {
            if (now <= highlight.expiresAt()) {
                int color = highlight.color();
                drawTranslucentBoxFaces(poseStack, filledConsumer, highlight.box(),
                        color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 76);
            }
        }
        bufferSource.endBatch(CustomRenderTypes.HIGHLIGHT_BOX);

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        for (ModuleHighlightManager.Highlight highlight : highlights.values()) {
            if (now <= highlight.expiresAt()) {
                int color = highlight.color();
                LevelRenderer.renderLineBox(poseStack, lineConsumer, highlight.box(),
                        (color >> 16 & 0xFF) / 255.0f, (color >> 8 & 0xFF) / 255.0f, (color & 0xFF) / 255.0f, 0.8f);
            }
        }
        bufferSource.endBatch(RenderType.lines());

        poseStack.popPose();
    }

    private static void drawTranslucentBoxFaces(PoseStack poseStack, VertexConsumer consumer, AABB aabb, int r, int g, int b, int a) {
        float minX = (float) aabb.minX; float minY = (float) aabb.minY; float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX; float maxY = (float) aabb.maxY; float maxZ = (float) aabb.maxZ;
        var matrix = poseStack.last().pose();

        consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
    }
}
