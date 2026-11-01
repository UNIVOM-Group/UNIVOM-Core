package com.mrlacar.univom.common.pipelike.beam;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.api.pipenet.PipeNet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BeamPipeNet extends PipeNet<BeamPipeProperties> {
    private final Map<BlockPos, BeamRoutePath> NET_DATA = new Object2ObjectOpenHashMap<>();

    public BeamPipeNet(LevelPipeNet<BeamPipeProperties, ? extends PipeNet<BeamPipeProperties>> world) {
        super(world);
    }

    @Nullable
    public BeamRoutePath getNetData(BlockPos pipePos, Direction facing) {
        if (NET_DATA.containsKey(pipePos)) return NET_DATA.get(pipePos);

        BeamRoutePath data = BeamNetWalker.createNetData(this, pipePos, facing);
        if (data == BeamNetWalker.FAILED_MARKER) return null;

        NET_DATA.put(pipePos, data);
        return data;
    }

    @Override
    public void onNeighbourUpdate(BlockPos fromPos) {
        NET_DATA.clear();
    }

    @Override
    public void onPipeConnectionsUpdate() {
        NET_DATA.clear();
    }

    @Override
    protected void transferNodeData(Map<BlockPos, Node<BeamPipeProperties>> transferredNodes, PipeNet<BeamPipeProperties> parentNet) {
        super.transferNodeData(transferredNodes, parentNet);
        NET_DATA.clear();
        ((BeamPipeNet) parentNet).NET_DATA.clear();
    }

    @Override
    protected void writeNodeData(BeamPipeProperties nodeData, CompoundTag tagCompound) {}

    @Override
    protected BeamPipeProperties readNodeData(CompoundTag tagCompound) {
        return BeamPipeProperties.INSTANCE;
    }
}
