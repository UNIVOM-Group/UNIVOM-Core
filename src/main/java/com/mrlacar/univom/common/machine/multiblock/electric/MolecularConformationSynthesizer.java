package com.mrlacar.univom.common.machine.multiblock.electric;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.trait.GridNodeHolder;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.common.machine.trait.MolecularConformationSynthesizerLogic;
import com.mrlacar.univom.integration.ae2.machine.MolecularConformationSynthesizerMEPatternBufferPartMachine;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MolecularConformationSynthesizer extends WorkableElectricMultiblockMachine implements IGridConnectedMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MolecularConformationSynthesizer.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    protected final GridNodeHolder nodeHolder;

    protected final IActionSource actionSource;

    @DescSynced
    protected boolean isOnline = false;

    private MolecularConformationSynthesizerMEPatternBufferPartMachine patternBufferPart;
    private ParallelHatchPartMachine parallelHatchPart;

    private final Object2LongMap<AEKey> requiredPerCraft = new Object2LongOpenHashMap<>();
    private final Object2LongMap<AEKey> outputsPerCraft = new Object2LongOpenHashMap<>();

    public MolecularConformationSynthesizer(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.nodeHolder = new GridNodeHolder(this);
        this.actionSource = IActionSource.ofMachine(this.nodeHolder.getMainNode()::getNode);

        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getMainNode().setVisualRepresentation(this.getDefinition().getItem());
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        for (var part : getParts()) {
            if (part instanceof MolecularConformationSynthesizerMEPatternBufferPartMachine bufferPart) {
                this.patternBufferPart = bufferPart;
            } else if (part instanceof ParallelHatchPartMachine parallelHatch) {
                this.parallelHatchPart = parallelHatch;
            }
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.patternBufferPart = null;
        this.parallelHatchPart = null;
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        if (getMainNode() != null) {
            getMainNode().setExposedOnSides(EnumSet.of(newFacing));
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!this.isOnline()) {
            return false;
        }
        return super.beforeWorking(recipe);
    }

    @Nullable
    public GTRecipe getDynamicRecipe() {
        if (!this.isOnline() || patternBufferPart == null) return null;

        long maxParallel = 1;
        if (this.parallelHatchPart != null) {
            maxParallel = this.parallelHatchPart.getCurrentParallel();
        }

        for (var entry : patternBufferPart.getDetailsSlotMap().entrySet()) {
            IPatternDetails pattern = entry.getKey();
            var slot = entry.getValue();

            if (slot.isItemEmpty()) continue;

            boolean hasInputs = false;
            for (var patternIn : pattern.getInputs()) {
                if (patternIn != null && patternIn.getPossibleInputs() != null && patternIn.getPossibleInputs().length > 0) {
                    hasInputs = true;
                    break;
                }
            }
            if (!hasInputs) continue;

            this.requiredPerCraft.clear();
            for (var patternIn : pattern.getInputs()) {
                var possible = patternIn.getPossibleInputs()[0];
                if (possible == null || possible.what() == null) continue;

                long amount = possible.amount() * patternIn.getMultiplier();
                this.requiredPerCraft.put(possible.what(), this.requiredPerCraft.getLong(possible.what()) + amount);
            }

            long possibleCrafts = Long.MAX_VALUE;
            for (var reqEntry : this.requiredPerCraft.object2LongEntrySet()) {
                AEKey key = reqEntry.getKey();
                long reqAmount = reqEntry.getLongValue();
                if (reqAmount <= 0) continue;

                long available = 0;
                if (key instanceof AEItemKey aeItem) {
                    available = slot.getAvailableAmount(aeItem);
                }

                possibleCrafts = Math.min(possibleCrafts, available / reqAmount);
            }

            if (possibleCrafts == Long.MAX_VALUE || possibleCrafts <= 0) continue;

            long craftsToProcess = Math.min(possibleCrafts, maxParallel);

            GTRecipeBuilder builder = GTRecipeBuilder.ofRaw().duration(this.tier * 8).EUt(120);

            for (var reqEntry : this.requiredPerCraft.object2LongEntrySet()) {
                if (reqEntry.getKey() instanceof AEItemKey aeItem) {
                    long totalInput = reqEntry.getLongValue() * craftsToProcess;
                    int safeInput = (int) Math.min(totalInput, Integer.MAX_VALUE);
                    builder.inputItems(SizedIngredient.create(aeItem.toStack(safeInput)));
                }
            }

            this.outputsPerCraft.clear();
            for (var patternOut : pattern.getOutputs()) {
                if (patternOut == null || patternOut.what() == null) continue;
                this.outputsPerCraft.put(patternOut.what(), this.outputsPerCraft.getLong(patternOut.what()) + patternOut.amount());
            }

            for (var outEntry : this.outputsPerCraft.object2LongEntrySet()) {
                if (outEntry.getKey() instanceof AEItemKey aeItem) {
                    long totalOutput = outEntry.getLongValue() * craftsToProcess;
                    int safeOutput = (int) Math.min(totalOutput, Integer.MAX_VALUE);
                    builder.outputItems(aeItem.toStack(safeOutput));
                }
            }

            return builder.buildRawRecipe();
        }

        return null;
    }

    @Override
    protected @NotNull RecipeLogic createRecipeLogic(Object @NotNull... args) {
        return new MolecularConformationSynthesizerLogic(this);
    }

    @Override
    public IManagedGridNode getMainNode() {
        return nodeHolder.getMainNode();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        IGridConnectedMachine.super.onMainNodeStateChanged(reason);
    }

    @Override
    public void setOnline(boolean online) {
        this.isOnline = online;
        if (!online && getRecipeLogic().isWorking()) {
            getRecipeLogic().interruptRecipe();
        }
    }
}