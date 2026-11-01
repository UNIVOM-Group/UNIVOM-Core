package com.mrlacar.univom.common.machine.trait;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.mrlacar.univom.common.machine.multiblock.electric.MolecularConformationSynthesizer;

public class MolecularConformationSynthesizerLogic extends RecipeLogic {
    private final MolecularConformationSynthesizer machine;

    private boolean loggedStatusFailure = false;

    public MolecularConformationSynthesizerLogic(MolecularConformationSynthesizer machine) {
        super(machine);
        this.machine = machine;
    }

    @Override
    public void findAndHandleRecipe() {
        this.lastFailedMatches = null;
        this.recipeDirty = false;
        this.lastRecipe = null;
        this.lastOriginRecipe = null;

        GTRecipe recipe = machine.getDynamicRecipe();

        if (recipe != null) {
            setupRecipe(recipe);
        }
    }

    @Override
    public void setStatus(RecipeLogic.Status status) {
        try {
            super.setStatus(status);
        } catch (IllegalArgumentException e) {
            if (!loggedStatusFailure) {
                loggedStatusFailure = true;
                GTCEu.LOGGER.warn("MolecularConformationSynthesizer at {} failed to apply recipe status {}",
                        machine.getPos(), status, e);
            }
        }
    }
}
