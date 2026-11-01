package com.mrlacar.univom.common.pipelike.pressure;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public class LevelPressurePipeNet extends LevelPipeNet<PressurePipeProperties, PressurePipeNet> {
    private static final String DATA_ID = "univom_pressure_pipe_net";
    private static final int WARNING_INTERVAL = 20;
    private static final int MAX_BURSTS_PER_TICK = 2;
    private static final PressurePipeNet[] EMPTY = new PressurePipeNet[0];

    private static int burstTick = -1;
    private static int burstsLeft;

    private ReferenceOpenHashSet<PressurePipeNet> pending;
    private ReferenceOpenHashSet<PressurePipeNet> warnings;
    @Nullable
    private Effects effects;

    public static LevelPressurePipeNet getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                tag -> new LevelPressurePipeNet(serverLevel, tag),
                () -> new LevelPressurePipeNet(serverLevel), DATA_ID);
    }

    public LevelPressurePipeNet(ServerLevel level) {
        super(level);
        initMonitor();
    }

    public LevelPressurePipeNet(ServerLevel serverLevel, CompoundTag tag) {
        super(serverLevel, tag);
        initMonitor();
        pipeNets.forEach(this::watch);
    }

    @Override
    protected PressurePipeNet createNetInstance() {
        return new PressurePipeNet(this);
    }

    void watch(PressurePipeNet net) {
        if (pending != null && net.getAmount() > 0 && pending.add(net)) {
            PressureMonitor.activate(this);
        }
    }

    public boolean tick(int serverTick) {
        if (effects == null) {
            effects = new PressureEffects(getWorld());
        }
        return tick(serverTick, effects);
    }

    public boolean tick(int serverTick, Effects effects) {
        if (!pending.isEmpty()) {
            PressurePipeNet[] batch = pending.toArray(EMPTY);
            pending.clear();
            for (PressurePipeNet net : batch) {
                evaluate(net, serverTick, effects);
            }
        }
        if (!warnings.isEmpty() && serverTick % WARNING_INTERVAL == 0) {
            for (PressurePipeNet net : warnings.toArray(EMPTY)) {
                if (!net.isValid() || !net.isWarning()) {
                    warnings.remove(net);
                } else if (net.isOverpressured()) {
                    pending.add(net);
                } else {
                    BlockPos weakest = net.findWeakest(effects::isLoaded);
                    if (weakest != null) {
                        effects.warn(net, weakest);
                    }
                }
            }
        }
        return !pending.isEmpty() || !warnings.isEmpty();
    }

    private void evaluate(PressurePipeNet net, int serverTick, Effects effects) {
        if (!net.isValid() || !net.isWarning()) {
            warnings.remove(net);
            return;
        }
        if (!net.isOverpressured()) {
            warnings.add(net);
            return;
        }
        BlockPos weakest = net.findWeakest(effects::isLoaded);
        if (weakest == null) {
            warnings.add(net);
            return;
        }
        if (!takeBurst(serverTick)) {
            pending.add(net);
            return;
        }
        warnings.remove(net);
        effects.burst(net, weakest);
    }

    private static boolean takeBurst(int serverTick) {
        if (serverTick != burstTick) {
            burstTick = serverTick;
            burstsLeft = MAX_BURSTS_PER_TICK;
        }
        if (burstsLeft <= 0) {
            return false;
        }
        burstsLeft--;
        return true;
    }

    private void initMonitor() {
        pending = new ReferenceOpenHashSet<>();
        warnings = new ReferenceOpenHashSet<>();
    }

    public interface Effects {

        boolean isLoaded(BlockPos pos);

        void warn(PressurePipeNet net, BlockPos pos);

        void burst(PressurePipeNet net, BlockPos pos);
    }
}
