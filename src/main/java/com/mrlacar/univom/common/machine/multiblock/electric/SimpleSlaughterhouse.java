package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mrlacar.univom.common.machine.multiblock.part.SlaughterhouseBusPartMachine;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleSlaughterhouse extends WorkableElectricMultiblockMachine {
    @NotNull
    private AABB damageBound = new AABB(BlockPos.ZERO);
    @NotNull
    private final List<SlaughterhouseBusPartMachine> busPartMachine = new ArrayList<>();

    private TickableSubscription hurtSub;

    public SimpleSlaughterhouse(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        updateBounds();
        for (IMultiPart part : getParts()) {
            if (part instanceof SlaughterhouseBusPartMachine machine) {
                this.busPartMachine.add(machine);
            }
        }
        hurtSub = subscribeServerTick(this::hurtEntities);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (hurtSub != null) {
            hurtSub.unsubscribe();
            hurtSub = null;
        }
        busPartMachine.clear();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (hurtSub != null) {
            hurtSub.unsubscribe();
            hurtSub = null;
        }
        busPartMachine.clear();
    }

    private void updateBounds() {
        var fl = RelativeDirection.offsetPos(getPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(), 0, 2, -1);
        var br = RelativeDirection.offsetPos(getPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(), 0, -3, -6);
        damageBound = AABB.of(BoundingBox.fromCorners(fl, br));
    }

    private void hurtEntities() {
        if (isRemote() || getLevel() == null) return;
        if (getOffsetTimer() % 10 != 0) return;

        if (!getRecipeLogic().isWorking()) return;

        float damage = Math.min(40.0f, Math.max(1, this.getTier()) * 5.0f);

        List<ItemEntity> itemEntities = new ArrayList<>();
        for (var entity : getLevel().getEntities(null, damageBound)) {
            if (entity instanceof ItemEntity ie) {
                itemEntities.add(ie);
            } else {
                if (recipeLogic.isWorking()) {
                    entity.hurt(entity.damageSources().cramming(), damage);
                }
            }
        }

        if (busPartMachine.isEmpty()) return;

        for (ItemEntity item : itemEntities) {
            if (item.isRemoved()) continue;
            for (var holder : busPartMachine) {
                item.setItem(ItemHandlerHelper.insertItem(holder.getInventory(), item.getItem(), false));
                if (item.getItem().isEmpty()) {
                    item.discard();
                    break;
                }
            }
        }
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new runningLogic(this);
    }

    public static class runningLogic extends RecipeLogic {

        private GTRecipe recipe = null;

        public runningLogic(IRecipeLogicMachine machine) {
            super(machine);
        }

        @Override
        public SimpleSlaughterhouse getMachine() {
            return (SimpleSlaughterhouse) super.getMachine();
        }

        @Override
        public void findAndHandleRecipe() {
            this.lastFailedMatches = null;
            this.recipeDirty = false;
            this.lastRecipe = null;
            this.lastOriginRecipe = null;

            if (getMachine().getLevel() == null || getMachine().getLevel().isClientSide) return;
            if (!getMachine().isFormed()) return;

            if (recipe == null) {
                recipe = GTRecipeBuilder.ofRaw()
                        .duration(1200)
                        .EUt(GTValues.VA[GTValues.LV])
                        .circuitMeta(1)
                        .buildRawRecipe();
            }

            if (RecipeHelper.matchContents(getMachine(), recipe).isSuccess()) {
                setupRecipe(recipe);
            }
        }
    }
}