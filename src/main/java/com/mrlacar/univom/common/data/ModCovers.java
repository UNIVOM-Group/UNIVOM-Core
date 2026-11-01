package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.client.renderer.cover.ICoverRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.IOCoverRenderer;
import com.gregtechceu.gtceu.common.cover.ConveyorCover;
import com.gregtechceu.gtceu.common.cover.FluidRegulatorCover;
import com.gregtechceu.gtceu.common.cover.PumpCover;
import com.gregtechceu.gtceu.common.cover.RobotArmCover;
import com.mrlacar.univom.UNIVOM;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.function.Supplier;

public class ModCovers {
    public static final List<CoverDefinition> COVERS = new ObjectArrayList<>();

    public static CoverDefinition ULV_CONVEYOR = registerULVCover("conveyor", ConveyorCover::new,
            () -> () -> new IOCoverRenderer(
                    GTCEu.id("block/cover/conveyor"),
                    null,
                    GTCEu.id("block/cover/conveyor_emissive"),
                    GTCEu.id("block/cover/conveyor_inverted_emissive")));

    public static CoverDefinition ULV_ROBOT_ARM = registerULVCover("robot_arm", RobotArmCover::new,
            () -> () -> new IOCoverRenderer(
                    GTCEu.id("block/cover/arm"),
                    null,
                    GTCEu.id("block/cover/arm_emissive"),
                    GTCEu.id("block/cover/arm_inverted_emissive")));

    public static CoverDefinition ULV_PUMP = register("pump", (def, cov, side) ->
                    new PumpCover(def, cov, side, GTValues.ULV, 32),
            () -> () -> IOCoverRenderer.PUMP_LIKE_COVER_RENDERER);

    public static CoverDefinition ULV_FLUID_REGULATOR = register("fluid_regulator",
            (def, cov, side) -> new FluidRegulatorCover(def, cov, side, GTValues.ULV, 32),
            () -> () -> IOCoverRenderer.PUMP_LIKE_COVER_RENDERER);

    public static CoverDefinition register(String id, CoverDefinition.CoverBehaviourProvider behavior, Supplier<Supplier<ICoverRenderer>> renderer) {
        var definition = new CoverDefinition(UNIVOM.id(id), behavior, renderer);
        COVERS.add(definition);
        return definition;
    }

    public static CoverDefinition registerULVCover(String id, CoverDefinition.TieredCoverBehaviourProvider behavior, Supplier<Supplier<ICoverRenderer>> renderer) {
        return register(id, (def, cov, side) -> behavior.create(def, cov, side, GTValues.ULV), renderer);
    }
}
