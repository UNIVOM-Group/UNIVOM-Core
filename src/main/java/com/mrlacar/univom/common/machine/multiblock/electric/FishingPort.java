package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.mrlacar.univom.common.machine.trait.FishingPortLogic;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FishingPort extends WorkableElectricMultiblockMachine {
    public FishingPort(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new FishingPortLogic(this);
    }

    @NotNull
    @Override
    public FishingPortLogic getRecipeLogic() {
        return (FishingPortLogic) super.getRecipeLogic();
    }
}
