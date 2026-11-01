package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.machine.fancyconfigurator.LargeHadronColliderEnergyConfigurator;
import com.mrlacar.univom.api.machine.fancyconfigurator.LargeHadronColliderPredictionConfigurator;
import com.mrlacar.univom.api.machine.feature.IModuleHighlightMachine;
import com.mrlacar.univom.api.machine.multiblock.LargeHadronColliderModuleType;
import com.mrlacar.univom.api.machine.trait.NotifiableBeamContainer;
import com.mrlacar.univom.common.machine.multiblock.part.BeamHatchPartMachine;
import com.mrlacar.univom.common.machine.trait.LargeHadronColliderLogic;
import com.mrlacar.univom.world.client.ModuleHighlightManager;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LargeHadronCollider extends WorkableElectricMultiblockMachine implements IModuleHighlightMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LargeHadronCollider.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Nullable
    @Persisted
    @DescSynced
    private BlockPos posCMS = null;

    @Nullable
    @Persisted
    @DescSynced
    private BlockPos posATLAS = null;

    @Nullable
    @Persisted
    @DescSynced
    private BlockPos posALICE = null;

    @Nullable
    @Persisted
    @DescSynced
    private BlockPos posLHCB = null;

    @Getter
    @Persisted
    @DescSynced
    private double targetEnergy1 = 0.0;

    @Getter
    @Persisted
    @DescSynced
    private double targetEnergy2 = 0.0;

    private final Map<LargeHadronColliderModuleType, LargeHadronColliderModule> cachedModules = new EnumMap<>(LargeHadronColliderModuleType.class);

    private final List<NotifiableBeamContainer> beamInputHatches = new ArrayList<>();
    private boolean beamInputsDirty = true;

    @Nullable
    protected TickableSubscription bindSubscription;

    public LargeHadronCollider(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        this.beamInputsDirty = true;
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.beamInputHatches.clear();
        this.beamInputsDirty = true;
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        this.beamInputHatches.clear();
        this.beamInputsDirty = true;
    }

    public List<NotifiableBeamContainer> getBeamInputHatches() {
        if (this.beamInputsDirty) {
            this.beamInputsDirty = false;
            this.beamInputHatches.clear();
            for (IMultiPart part : getParts()) {
                if (part instanceof BeamHatchPartMachine hatch && !hatch.isTransmitter()) {
                    this.beamInputHatches.add(hatch.getBeamContainer());
                }
            }
        }
        return this.beamInputHatches;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            bindSubscription = subscribeServerTick(bindSubscription, this::checkAndBindModules);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (bindSubscription != null) {
            bindSubscription.unsubscribe();
            bindSubscription = null;
        }
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new LargeHadronColliderLogic(this);
    }

    @Override
    public boolean isRecipeLogicAvailable() {
        return super.isRecipeLogicAvailable() &&
                (posCMS != null || posATLAS != null || posALICE != null || posLHCB != null);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        this.attachHighlightConfigurator(configuratorPanel);
        configuratorPanel.attachConfigurators(new LargeHadronColliderEnergyConfigurator(this));
        configuratorPanel.attachConfigurators(new LargeHadronColliderPredictionConfigurator(this));
        super.attachConfigurators(configuratorPanel);
    }

    @Override
    public void triggerHighlight(ClickData clickData) {
        if (clickData.isRemote) {
            for (BlockPos pos : getTargetBindingPositions()) {
                ModuleHighlightManager.addHighlight(pos);
            }
        }
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);

        if (isFormed()) {
            if (posCMS == null && posATLAS == null && posALICE == null && posLHCB == null) {
                textList.add(Component.literal(" "));
                textList.add(Component.translatable("univom.multiblock.large_hadron_collider.no_modules_warning").withStyle(ChatFormatting.RED));
            }

            int beamIndex = 1;
            for (IMultiPart part : getParts()) {
                if (part instanceof BeamHatchPartMachine beamHatch && !beamHatch.isTransmitter()) {
                    NotifiableBeamContainer container = beamHatch.getBeamContainer();
                    List<Object> contents = container.getContents();

                    if (contents == null || contents.isEmpty() || !(contents.get(0) instanceof BeamPacket packet) || packet.flux() <= 0) {
                        textList.add(Component.translatable("univom.multiblock.large_hadron_collider.hatch_energy_indexed", beamIndex, "0.00 MeV").withStyle(ChatFormatting.GREEN));
                        textList.add(Component.translatable("univom.multiblock.large_hadron_collider.hatch_flux_indexed", beamIndex, 0).withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else {
                        double beamEnergy = packet.energy();
                        String energyString;
                        if (beamEnergy >= 1000000.0) {
                            energyString = String.format("%.2f TeV", beamEnergy / 1000000.0);
                        } else if (beamEnergy >= 1000.0) {
                            energyString = String.format("%.2f GeV", beamEnergy / 1000.0);
                        } else {
                            energyString = String.format("%.2f MeV", beamEnergy);
                        }

                        textList.add(Component.translatable("univom.multiblock.large_hadron_collider.hatch_energy_indexed", beamIndex, energyString).withStyle(ChatFormatting.GREEN));
                        textList.add(Component.translatable("univom.multiblock.large_hadron_collider.hatch_flux_indexed", beamIndex,
                                        FormattingUtil.formatNumbers(packet.flux()))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    beamIndex++;
                }
            }
        }
    }

    public void setTargetEnergy1(int val) {
        this.targetEnergy1 = val;
    }

    public void setTargetEnergy2(int val) {
        this.targetEnergy2 = val;
    }

    private BlockPos[] getTargetBindingPositions() {
        Direction back = getFrontFacing().getOpposite();
        Direction left = RelativeDirection.LEFT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction right = RelativeDirection.RIGHT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        BlockPos root = getPos();
        return new BlockPos[] {
                root.relative(back, 6).relative(left, 25),
                root.relative(back, 50).relative(right, 19),
                root.relative(back, 50).relative(left, 69),
                root.relative(back, 94).relative(left, 25)
        };
    }

    private void checkAndBindModules() {
        if (getOffsetTimer() % 20 != 0) return;

        BlockPos[] targets = getTargetBindingPositions();
        Map<LargeHadronColliderModuleType, LargeHadronColliderModule> foundThisTick = new EnumMap<>(LargeHadronColliderModuleType.class);

        for (BlockPos pos : targets) {
            MetaMachine machine = MetaMachine.getMachine(getLevel(), pos);
            if (machine instanceof LargeHadronColliderModule module) {
                LargeHadronColliderModuleType type = module.getModuleType();
                if (!foundThisTick.containsKey(type)) {
                    foundThisTick.put(type, module);
                    module.setDuplicate(false);
                } else {
                    module.setDuplicate(true);
                    module.unbindFromHost();
                }
            }
        }

        posCMS = updateSpecificModule(LargeHadronColliderModuleType.CMS, foundThisTick.get(LargeHadronColliderModuleType.CMS), posCMS);
        posATLAS = updateSpecificModule(LargeHadronColliderModuleType.ATLAS, foundThisTick.get(LargeHadronColliderModuleType.ATLAS), posATLAS);
        posALICE = updateSpecificModule(LargeHadronColliderModuleType.ALICE, foundThisTick.get(LargeHadronColliderModuleType.ALICE), posALICE);
        posLHCB = updateSpecificModule(LargeHadronColliderModuleType.LHCB, foundThisTick.get(LargeHadronColliderModuleType.LHCB), posLHCB);
    }

    private BlockPos updateSpecificModule(LargeHadronColliderModuleType type, LargeHadronColliderModule foundModule, BlockPos oldPos) {
        if (foundModule != null) {
            BlockPos newPos = foundModule.getPos();
            if (oldPos == null || !oldPos.equals(newPos)) {
                foundModule.bindToHost(this.getPos());
            }
            cachedModules.put(type, foundModule);
            return newPos;
        } else {
            if (oldPos != null) {
                MetaMachine oldMachine = MetaMachine.getMachine(getLevel(), oldPos);
                if (oldMachine instanceof LargeHadronColliderModule oldMod) {
                    oldMod.unbindFromHost();
                }
                if (getRecipeLogic().isWorking()) getRecipeLogic().interruptRecipe();
            }
            cachedModules.remove(type);
            return null;
        }
    }

    public void forceUnbindModule(BlockPos pos) {
        if (pos.equals(posCMS)) posCMS = null;
        if (pos.equals(posATLAS)) posATLAS = null;
        if (pos.equals(posALICE)) posALICE = null;
        if (pos.equals(posLHCB)) posLHCB = null;

        cachedModules.entrySet().removeIf(entry -> entry.getValue().getPos().equals(pos));
        if (getRecipeLogic().isWorking()) getRecipeLogic().interruptRecipe();
    }

    public void dispatchCollisionResult(LargeHadronColliderModuleType targetType, BeamPacket resultBeam) {
        LargeHadronColliderModule module = cachedModules.get(targetType);
        if (module != null && module.isFormed()) {
            module.injectBeamFromLHC(resultBeam);
        }
    }
}
