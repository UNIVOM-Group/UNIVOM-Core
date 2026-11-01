package com.mrlacar.univom.common.machine.trait;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.mrlacar.univom.common.machine.multiblock.electric.BeamDirectedImpactChamber;

public class BeamDirectedImpactChamberLogic extends RecipeLogic {
    public BeamDirectedImpactChamberLogic(BeamDirectedImpactChamber machine) {
        super(machine);
    }

    @Override
    public BeamDirectedImpactChamber getMachine() {
        return (BeamDirectedImpactChamber) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        if (!getMachine().isRecipeLogicAvailable() || !isWorkingEnabled()) return;
        super.findAndHandleRecipe();
    }
}
