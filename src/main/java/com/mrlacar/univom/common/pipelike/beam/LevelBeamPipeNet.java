package com.mrlacar.univom.common.pipelike.beam;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public class LevelBeamPipeNet extends LevelPipeNet<BeamPipeProperties, BeamPipeNet> {
    private static final String DATA_ID = "univom_beam_pipe_net";

    public static LevelBeamPipeNet getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                tag -> new LevelBeamPipeNet(serverLevel, tag),
                () -> new LevelBeamPipeNet(serverLevel), DATA_ID);
    }

    public LevelBeamPipeNet(ServerLevel level) {
        super(level);
    }

    public LevelBeamPipeNet(ServerLevel serverLevel, CompoundTag tag) {
        super(serverLevel, tag);
    }

    @Override
    protected BeamPipeNet createNetInstance() {
        return new BeamPipeNet(this);
    }
}
