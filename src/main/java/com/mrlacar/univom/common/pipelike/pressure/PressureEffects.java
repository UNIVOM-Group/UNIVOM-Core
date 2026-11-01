package com.mrlacar.univom.common.pipelike.pressure;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.mojang.logging.LogUtils;
import com.mrlacar.univom.api.machine.feature.IPressureEndpoint;
import com.mrlacar.univom.common.block.PressurePipeBlock;
import com.mrlacar.univom.common.data.ModSoundEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Locale;

public final class PressureEffects implements LevelPressurePipeNet.Effects {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_POWER = 8.0D;

    private final ServerLevel level;

    PressureEffects(ServerLevel level) {
        this.level = level;
    }

    public static void vent(ServerLevel level, BlockPos pos, double load) {
        double strength = Math.max(0.0D, Math.min(1.0D, load));
        level.playSound(null, pos, ModSoundEntries.PRESSURE_VENT.getMainEvent(), SoundSource.BLOCKS,
                0.6F + 0.4F * (float) strength, 0.55F + level.random.nextFloat() * 0.1F);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8 + (int) Math.round(16.0D * strength), 0.3D, 0.3D, 0.3D, 0.05D + 0.1D * strength);
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return level.isLoaded(pos);
    }

    @Override
    public void warn(PressurePipeNet net, BlockPos pos) {
        RandomSource random = level.random;
        level.playSound(null, pos, ModSoundEntries.PRESSURE_WARNING.getMainEvent(), SoundSource.BLOCKS, 0.4F,
                1.6F + random.nextFloat() * 0.3F);
        Direction side = Direction.getRandom(random);
        double x = pos.getX() + 0.5D + side.getStepX() * 0.55D;
        double y = pos.getY() + 0.5D + side.getStepY() * 0.55D;
        double z = pos.getZ() + 0.5D + side.getStepZ() * 0.55D;
        int puffs = 2 + random.nextInt(3);
        for (int i = 0; i < puffs; i++) {
            level.sendParticles(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5D) * 0.2D,
                    y + (random.nextDouble() - 0.5D) * 0.2D, z + (random.nextDouble() - 0.5D) * 0.2D, 0,
                    side.getStepX(), side.getStepY(), side.getStepZ(), 0.08D + random.nextDouble() * 0.04D);
        }
    }

    @Override
    public void burst(PressurePipeNet net, BlockPos pos) {
        if (net.containsNode(pos)) {
            if (level.getBlockState(pos).getBlock() instanceof PressurePipeBlock block) {
                explode(net, pos, block.getPressureType().getBurstFactor());
            } else {
                LevelPressurePipeNet.getOrCreate(level).removeNode(pos);
            }
            return;
        }
        PressurePipeNet.Attachment attachment = net.getAttachment(pos);
        if (attachment == null) {
            return;
        }
        if (MetaMachine.getMachine(level, pos) instanceof IPressureEndpoint) {
            explode(net, pos, attachment.burstFactor());
        } else {
            net.detach(pos);
        }
    }

    private void explode(PressurePipeNet net, BlockPos pos, float burstFactor) {
        double pressure = net.getPressure();
        int rating = net.getMinRating();
        float power = (float) Math.min(MAX_POWER, 1.2D * Math.cbrt(pressure) * burstFactor);
        net.vent();
        level.removeBlock(pos, false);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.explode(null, x, y, z, power, ConfigHolder.INSTANCE.machines.doesExplosionDamagesTerrain ?
                Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 24, 0.5D, 0.5D, 0.5D, 0.15D);
        LOGGER.info("Pressure burst at {} in {}: {} bar against a {} bar rating, explosion power {}",
                pos.toShortString(), level.dimension().location(), String.format(Locale.ROOT, "%.1f", pressure),
                rating, String.format(Locale.ROOT, "%.2f", power));
    }
}
