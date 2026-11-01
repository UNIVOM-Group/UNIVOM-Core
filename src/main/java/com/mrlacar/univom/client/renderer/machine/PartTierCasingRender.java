package com.mrlacar.univom.client.renderer.machine;

import com.mrlacar.univom.api.machine.multiblock.PartTierWorkableElectricMultiblockMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.client.model.machine.IControllerModelRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.ModelUtils;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PartTierCasingRender extends DynamicRender<MultiblockControllerMachine, PartTierCasingRender>
                                  implements IControllerModelRenderer {

    public static final Codec<PartTierCasingRender> CODEC = Codec.unit(PartTierCasingRender::new);
    public static final DynamicRenderType<MultiblockControllerMachine, PartTierCasingRender> TYPE =
            new DynamicRenderType<>(CODEC);

    @Override
    public @NotNull DynamicRenderType<MultiblockControllerMachine, PartTierCasingRender> getType() {
        return TYPE;
    }

    @Override
    public void render(MultiblockControllerMachine machine, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {}

    @Override
    public boolean isBlockEntityRenderer() {
        return false;
    }

    @Override
    public void renderPartModel(List<BakedQuad> quads, IMultiController controller, IMultiPart part,
                                Direction frontFacing, Direction side, RandomSource random, ModelData modelData,
                                RenderType renderType) {
        if (!controller.isFormed() ||
                !(controller.self() instanceof PartTierWorkableElectricMultiblockMachine machine)) {
            return;
        }
        BlockState appearance = machine.getAppearanceState();
        if (appearance == null) {
            return;
        }
        MetaMachine self = part.self();
        Level level = self.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = self.getPos();
        BakedModel model = ModelUtils.getModelForState(appearance);
        quads.addAll(model.getQuads(appearance, side, random,
                model.getModelData(level, pos, appearance, modelData), renderType));
    }
}
