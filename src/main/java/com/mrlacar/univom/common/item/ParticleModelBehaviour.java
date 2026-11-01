package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ParticleModelBehaviour implements IAddInformation {
    private final ParticleDefinition particleDef;

    public ParticleModelBehaviour(ParticleDefinition particleDef) {
        this.particleDef = particleDef;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (this.particleDef != null) this.particleDef.addInformation(tooltipComponents);
    }
}
